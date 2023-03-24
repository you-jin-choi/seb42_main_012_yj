import { useNavigate } from 'react-router-dom';

function TabButton({ tabName, nav }) {
  const navigate = useNavigate();
  const buttonHandler = () => {
    navigate(nav);
  };
  return (
    <>
      <button
        type="button"
        className="border border-[var(--second)] w-24 rounded-full flex justify-center mr-2 text-[var(--second)]"
        onClick={buttonHandler}
      >
        {tabName}
      </button>
    </>
  );
}

export default TabButton;
