import { Navigate, Route, Routes } from 'react-router-dom'
import { HOME, useAuth } from './auth/AuthContext.jsx'
import Layout from './components/Layout.jsx'
import { GuestOnly, RequireAuth, RequireRole, Splash } from './components/RouteGuards.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import ResidentDashboard from './pages/ResidentDashboard.jsx'
import OfficerDashboard from './pages/OfficerDashboard.jsx'
import OfficerResidents from './pages/OfficerResidents.jsx'
import AdminDashboard from './pages/AdminDashboard.jsx'
import AdminOfficers from './pages/AdminOfficers.jsx'
import Profile from './pages/Profile.jsx'
import ChangePassword from './pages/ChangePassword.jsx'
import ForgotPassword from './pages/ForgotPassword.jsx'
import ResetPassword from './pages/ResetPassword.jsx'
import { Forbidden, NotFound } from './pages/Errors.jsx'

function Root() {
  const { user, loading } = useAuth()
  if (loading) return <Splash />
  return <Navigate to={user ? HOME[user.role] : '/login'} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Root />} />
      <Route element={<GuestOnly />}>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Navigate to="/register/resident" replace />} />
        <Route path="/register/:kind" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
      </Route>
      <Route path="/reset-password" element={<ResetPassword />} />

      <Route element={<RequireAuth />}>
        <Route path="/app" element={<Layout />}>
          <Route path="profile" element={<Profile />} />
          <Route path="change-password" element={<ChangePassword />} />
          <Route element={<RequireRole roles={['RESIDENT']} />}>
            <Route path="resident" element={<ResidentDashboard />} />
          </Route>
          <Route element={<RequireRole roles={['OFFICER']} />}>
            <Route path="officer" element={<OfficerDashboard />} />
            <Route path="officer/residents" element={<OfficerResidents />} />
          </Route>
          <Route element={<RequireRole roles={['ADMIN']} />}>
            <Route path="admin" element={<AdminDashboard />} />
            <Route path="admin/officers" element={<AdminOfficers />} />
          </Route>
        </Route>
        <Route path="/forbidden" element={<Forbidden />} />
      </Route>
      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
