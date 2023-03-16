package main012.server.user.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import main012.server.common.Auditable;
import main012.server.community.entity.Community;
import main012.server.community.entity.CommunityBookmark;
import main012.server.community.entity.CommunityComment;
import main012.server.gym.entity.Gym;
import main012.server.gym.entity.GymBookmark;
import main012.server.gym.entity.GymReview;
import main012.server.image.entity.Image;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends Auditable {
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String displayName;

    @Column(nullable = false, updatable = false, unique = true) // email 수정 불가, 유니크 O
    private String email;

    @Column(length = 100, nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UserStatus userStatus = UserStatus.USER_ACTIVE;

    // N : N
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // 1 : 1
    // 오펀 붙어 있어야 user.setImage(null) 했을 때, image 테이블에서 매핑된 로우값이 삭제됨.
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "image_id")
    private Image image;

    // 1 : N
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Community> communities = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CommunityBookmark> communityBookmarks = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<CommunityComment> communityComments = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Gym> gyms = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<GymBookmark> gymBookmarks = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<GymReview> gymReviews = new ArrayList<>();

    /*
     * 양방향 매핑 설정
     */
    public void setCommunity(Community community) {
        this.communities.add(community);
        if (community.getUser() != this) {
            community.setUser(this);
        }
    }

    public void setCommunityBookmark(CommunityBookmark communityBookmark) {
        this.communityBookmarks.add(communityBookmark);
        if (communityBookmark.getUser() != this) {
            communityBookmark.setUser(this);
        }
    }

    public void setCommunityComment(CommunityComment communityComment) {
        this.communityComments.add(communityComment);
        if (communityComment.getUser() != this) {
            communityComment.setUser(this);
        }
    }

    public void setGym(Gym gym) {
        this.gyms.add(gym);
        if (gym.getUser() != this) {
            gym.setUser(this);
        }
    }

    public void setGymBookmark(GymBookmark gymBookmark) {
        this.gymBookmarks.add(gymBookmark);
        if (gymBookmark.getUser() != this) {
            gymBookmark.setUser(this);
        }
    }

    public void setGymReview(GymReview gymReview) {
        this.gymReviews.add(gymReview);
        if (gymReview.getUser() != this) {
            gymReview.setUser(this);
        }
    }

    public enum UserStatus {
        USER_ACTIVE("활동중"),
        USER_SLEEP("휴먼 상태"),
        USER_DELETED("탈퇴 상태");

        @Getter
        private String status;

        UserStatus(String status) {
            this.status = status;
        }
    }
}
