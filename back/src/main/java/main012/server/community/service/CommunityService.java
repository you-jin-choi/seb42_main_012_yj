package main012.server.community.service;

import lombok.RequiredArgsConstructor;
import main012.server.community.dto.CommunityDto;
import main012.server.community.entity.Community;
import main012.server.community.entity.CommunityBookmark;
import main012.server.community.mapper.CommunityMapper;
import main012.server.community.repository.CommunityBookmarkRepository;
import main012.server.community.repository.CommunityRepository;
import main012.server.community.repository.TabRepository;
import main012.server.exception.BusinessLoginException;
import main012.server.exception.ExceptionCode;
import main012.server.image.entity.CommunityImage;
import main012.server.image.entity.Image;
import main012.server.image.repository.CommunityImageRepo;
import main012.server.image.repository.ImageRepository;
import main012.server.image.service.ImageService;
import main012.server.user.entity.Member;
import main012.server.user.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMapper communityMapper;
    private final TabRepository tabRepository;

    private final MemberRepository memberRepository;
    private final ImageService imageService;

    private final CommunityImageRepo communityImageRepo;

    private final CommunityBookmarkRepository communityBookmarkRepository;
    private final ImageRepository imageRepository;

    private final int size = 15;

    // 커뮤니티 게시글 등록
    public void createCommunity (CommunityDto.Post post, List<MultipartFile> files, Long memberId) throws IOException {

        // 첨부파일이 비었는지 체크
        boolean checkFiles = checkEmptyFile(files);

        if(post.getTabId() == 3 && checkFiles == true ){
            throw new BusinessLoginException(ExceptionCode.NO_IMAGE_ATTATCHED);
        }

        // dto로 넘어온 컨텐츠 community로 변환 후 저장
        Community community = communityMapper.communityPostDtoToCommunity(post, memberId);
        community.setTab(tabRepository.findById(post.getTabId()).get());


        // 첨부파일이 있으면 사진 업로드 기능 동작
        if(checkFiles != true ){
            List<Image>uploadedImages =imageService.upload(files, "upload");
            createCommunityImage(community, uploadedImages);
        }

        communityRepository.save(community);

    }


    // 게시글 등록시 파일이 비었는지 확인
    private boolean checkEmptyFile(List<MultipartFile> files) {
        for(MultipartFile multipartFile : files){
            if(multipartFile.isEmpty()) return true;
        }
        return false;
    }

    // communityImage에 정보 등록
    private void createCommunityImage(Community community, List<Image> images) {
        for(Image image :images) {
            CommunityImage communityImage = new CommunityImage(image, community);
            community.setCommunityImage(communityImage);
        }
    }

    // 커뮤니티 게시글 수정
    public Community updateCommunity (CommunityDto.Patch patchRequest, List<MultipartFile> files, Long memberId) throws IOException {



        // 게시글이 존재하는지 확인
        Community existCommunity = findExistCommunity(patchRequest.getCommunityId());

        // 작성자와 로그인한 사용자가 일치하는지 확인
        if(existCommunity.getMember().getId() != memberId){
            throw new BusinessLoginException(ExceptionCode.MEMBER_NOT_MATCHED);
        }

        //제목수정
        Optional.ofNullable(patchRequest.getTitle())
                .ifPresent(title -> existCommunity.setTitle(title));

        //내용수정
        Optional.ofNullable(patchRequest.getContent())
                .ifPresent(content -> existCommunity.setContent(content));

        // 탭수정
        Optional.ofNullable(patchRequest.getTabId())
                        .ifPresent(tabId -> existCommunity.setTab(tabRepository.findById(tabId).orElseThrow(()->new BusinessLoginException(ExceptionCode.TAB_NOT_FOUND))));

        /* 사진 수정 로직
        * 첨부파일이 있으면 patch에 담겨온 imageId 사진을 db, s3에서 지우고 -> 다시 등록하는 방식?
        * */

        // 첨부파일 비었는지 체크
        boolean checkFiles = checkEmptyFile(files);

        if(patchRequest.getDeletedCommunityImageId() != null){
            // 기존 사진 지우기
            for(Long value : patchRequest.getDeletedCommunityImageId()){
                CommunityImage communityImage = communityImageRepo.findById(value).orElseThrow(() -> new BusinessLoginException(ExceptionCode.IMAGE_NOT_FOUND));
                imageService.remove(communityImage.getImage());
                communityImageRepo.delete(communityImage);
            }
            if(checkFiles != true) {
                // 새로운 사진 등록
                List<Image> uploadedImages = imageService.upload(files, "upload");
                createCommunityImage(existCommunity, uploadedImages);
            }
        }

        return existCommunity;

    }

    // 커뮤니티 게시글 삭제
    public void deleteCommunity (Long communityId, Long memberId) {

        //존재하는 게시글인지 확인
        Community existCommunity = findExistCommunity(communityId);

        // 게시글 작성자와 로그인한 작성자가 일치하는지 확인
        if(existCommunity.getMember().getId() != memberId){
            throw new BusinessLoginException(ExceptionCode.MEMBER_NOT_MATCHED);
        }

        // 커뮤니티 게시글 번호로 저장된 이미지 찾기
        List<CommunityImage> communityImageList = communityImageRepo.findByCommunityCommunityId(communityId);

        // 사진 삭제
        for(CommunityImage value : communityImageList){
            imageService.remove(value.getImage());
            communityImageRepo.delete(value);
        }

        // 게시글 삭제
        communityRepository.delete(existCommunity);
    }

    // 게시글 상세 조회
    public CommunityDto.Response findCommunity (Long communityId, Long memberId) {

        //조회수 업데이트
        communityRepository.updateView(communityId);

        // 존재하는 게시글인지 확인
        Community foundCommunity = findExistCommunity(communityId);

        // 커뮤니티 게시글 번호로 저장된 이미지 찾기
        List<CommunityImage> communityImageList = communityImageRepo.findByCommunityCommunityId(communityId);


        // 컨텐츠에 저장된 이미지 id, url 응답데이터 추출
        List<CommunityDto.ImageResponse> imageInfo = new ArrayList<>();
        for(CommunityImage image : communityImageList){
            CommunityDto.ImageResponse response = new CommunityDto.ImageResponse();
            response.setContentImageId(image.getId());
            response.setContentImageUrl(image.getImage().getImagePath());
            imageInfo.add(response);
        }
        

        // 멤버 프로필 이미지 얻기 위한 멤버 조회
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessLoginException(ExceptionCode.MEMBER_NOT_FOUND));

        CommunityDto.Response response = communityMapper.communityToResponse(foundCommunity);
        response.setProfileImageUrl(member.getImage().getImagePath());
        response.setContentImages(imageInfo);

        return response;
    }

    // 게시글 전체 조회
    public CommunityDto.ListResponse findAllCommunity (String lastFeedId) {

        Long feedId = getFeedId(lastFeedId);

        Page<Community> list = communityRepository.findByCommunityIdLessThanOrderByCommunityIdDesc(feedId, PageRequest.of(0, size));
        List<Community> contents = list.getContent();


        int totalElements = contents.size();
        Long nextCursor = getNextCursor(contents, totalElements);

        // 북마크 되어 있는지 체크
        List<CommunityDto.AllCommunityResponse> responseList = communityMapper.communitiesToAllCommunityResponses(contents);
        for(CommunityDto.AllCommunityResponse value : responseList){
            Long communityId = value.getCommunityId();
            Long memberId = value.getMemberId();
            Optional<CommunityBookmark> isBookmarked = communityBookmarkRepository.findByMemberIdAndCommunityCommunityId(memberId, communityId);
            if(isBookmarked.isPresent()){
                value.setBookmarked(true);
            }
        }

        CommunityDto.ListResponse response = new CommunityDto.ListResponse();
        response.setContents(responseList);
        response.setTotalElements(totalElements);
        response.setNextCursor(nextCursor);

        return response;
    }


    // 게시글 검색 기능
    public CommunityDto.ListResponse findByKeyword (String keyword, String lastFeedId) {

        Long feedId = getFeedId(lastFeedId);


        Page<Community> foundCommunities = communityRepository.findByContentContainingAndCommunityIdLessThanOrderByCommunityIdDesc(keyword, feedId, PageRequest.of(0, size) );
        List<Community> contents = foundCommunities.getContent();

        int totalElements = contents.size();
        Long nextCursor = getNextCursor(contents, totalElements);


        // memberId, communityId로 게시물에 해당 멤버가 북마크 했는지 확인
        List<CommunityDto.AllCommunityResponse> responseList = communityMapper.communitiesToAllCommunityResponses(contents);
        for(CommunityDto.AllCommunityResponse value : responseList) {
            Long communityId = value.getCommunityId();
            Long memberId = value.getMemberId();
            Optional<CommunityBookmark> isBookmarked = communityBookmarkRepository.findByMemberIdAndCommunityCommunityId(memberId, communityId);
            if (isBookmarked.isPresent()) {
                value.setBookmarked(true);
            }
        }

        CommunityDto.ListResponse response = new CommunityDto.ListResponse();
        response.setContents(responseList);
        response.setTotalElements(totalElements);
        response.setNextCursor(nextCursor);

        return response;
    }

    // 게시글 탭별 조회 기능
    public CommunityDto.ListResponse findTabCommunities (Long tabId, String lastFeedId) {

        Long feedId = getFeedId(lastFeedId);

        int totalElements;

        List<Community> contents;

        String imagePath = null;

        if(tabId ==3){
            // 오운완 탭 조회시 검색되는 게시글 목록
            Page<Community> workoutTabCommunities = communityRepository.findAllByTabTabIdAndCommunityIdLessThanOrderByCommunityIdDesc(3L, feedId, PageRequest.of(0, size));
            contents = workoutTabCommunities.getContent();

        } else {
            // 일반 탭별 조회시 검색되는 게시글 목록
            Page<Community> communities =
                    communityRepository.findAllByTabTabIdAndCommunityIdLessThanOrderByCommunityIdDesc(tabId, feedId, PageRequest.of(0, size));
            contents = communities.getContent();
        }

        totalElements = contents.size();


        Long nextCursor = getNextCursor(contents, totalElements);



        if(tabId == 3){
            List<CommunityDto.WorkoutTabResponse> responseList = communityMapper.communitiesToWorkoutTabResponses(contents);
            // 오운완 탭 조회시 대표 사진 설정
            for(CommunityDto.WorkoutTabResponse value : responseList){
                CommunityImage communityImage = communityImageRepo.findByCommunityCommunityId(value.getCommunityId()).get(0);
                String firstImagePath = communityImage.getImage().getImagePath();
                value.setContentImageUrl(firstImagePath);
            }   // 응답 데이터 양식에 맞게 설정
            CommunityDto.ListResponse response = new CommunityDto.ListResponse();
            response.setContents(responseList);
            response.setTotalElements(totalElements);
            response.setNextCursor(nextCursor);

            return response;
        } else {    // 일반 탭 조회시 memberId, communityId로 게시물에 해당 멤버가 북마크 했는지 확인
        List<CommunityDto.TabListResponse> responseList = communityMapper.communitiesToTabListResponses(contents);
            for(CommunityDto.TabListResponse value : responseList){
                Long communityId = value.getCommunityId();
                Long memberId = value.getMemberId();
                Optional<CommunityBookmark> isBookmarked = communityBookmarkRepository.findByMemberIdAndCommunityCommunityId(memberId, communityId);
                if(isBookmarked.isPresent()){
                    value.setBookmarked(true);
                }
            }
        // 응답 데이터 양식에 맞게 설정
        CommunityDto.ListResponse response = new CommunityDto.ListResponse();
        response.setContents(responseList);
        response.setTotalElements(totalElements);
        response.setNextCursor(nextCursor);

        return response;
    }
    }

    // 페이지네이션 feedId 검증
    private Long getFeedId(String lastFeedId) {
        Long feedId;
        if (lastFeedId.isEmpty()) {
            feedId = 9223372036854775807L;
        } else if (!lastFeedId.matches("[+-]?\\d+")) {
            throw new BusinessLoginException(ExceptionCode.REQUEST_NOT_SUPPORT);
        } else {
            feedId = Long.valueOf(lastFeedId);
        }
        return feedId;
    }

    // nextCursor 얻기
    private Long getNextCursor(List<Community> contents, int totalElements) {
        Long nextCursor;
        if (totalElements < size) {
            nextCursor = -1L;
        } else {
            nextCursor = contents.get(size - 1).getCommunityId();
        }
        return nextCursor;
    }


    //존재하는 게시글인지 확인
    public Community findExistCommunity(Long communityId) {
        Optional<Community> optionalCommunity = communityRepository.findById(communityId);
        Community response = optionalCommunity.orElseThrow(
                () -> new BusinessLoginException(ExceptionCode.COMMUNITY_NOT_FOUND)
        );
        return response;
    }



}
