import GymNewest from './GymNewest';

function GymNewestList({ gyms }) {
  return (
    <div className="border-b border-[var(--second-border)]">
      <h2 className="text-[14px] font-bold">신규 헬스장</h2>
      <ul className="flex overflow-x-scroll scrollbar-hide">
        {gyms &&
          gyms.map(gym => (
            <GymNewest
              key={gym.gymId}
              gymId={gym.gymId}
              gymName={gym.gymName}
              gymImage={gym.gymImageUrl}
              isBookmarked={gym.isBookmarked}
            />
          ))}
      </ul>
    </div>
  );
}

export default GymNewestList;
