import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Home     from './pages/Home.jsx';
import Backtest from './pages/Backtest.jsx';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/"         element={<Home />} />
        <Route path="/backtest" element={<Backtest />} />
      </Routes>
    </BrowserRouter>
  );
}
