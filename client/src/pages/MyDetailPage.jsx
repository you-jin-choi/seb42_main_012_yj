import Header from '../components/layouts/Header/Header';
import Main from '../components/layouts/Main/Main';
import Nav from '../components/layouts/Nav/Nav';
import MyDetailTitle from '../components/My/MyDetailTitle';
import MyDetailList from '../components/My/MyDetailList';

function MyDetailPage({ text }) {
  return (
    <>
      <Header titleText="MY" />
      <Main>
        <MyDetailTitle text={text} />
        <MyDetailList />
      </Main>
      <Nav />
    </>
  );
}

export default MyDetailPage;
