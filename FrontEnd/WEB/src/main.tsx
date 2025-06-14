import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import App from "./UserManagement/Login";
import ResetPassword from "./UserManagement/ResetPassword";
import Dashboard from "./Dashboard";

import RequireAuth from "./UserManagement/RequireAuth";
import Layout from "./components/Layout";
import MyActivities from "./Events/ActivitiesList";
import EventDetails from "./Events/EventDetails";
import ChatList from "./Chat/ChatList";
import ChatRoom from "./Chat/ChatRoom";
import CreateEvent from "./Events/CreateEvent";
import Account from "./Perfil/Perfil";
import UserProfile from "./Perfil/userProfile";

import ChangePassword from "./UserManagement/ChangePassword";
import ChangeEmail from "./UserManagement/ChangeEmail";
import DeleteAccountPage from "./UserManagement/DeleteAccount";
import EditProfilePage from "./Perfil/EditProfilePage";
import EditEvent from "./Events/EditEvent";
import "./main.css";
import Leaderboard from "./Leaderboard";

const queryClient = new QueryClient();

ReactDOM.createRoot(document.getElementById("root")!).render(
  <QueryClientProvider client={queryClient}>
    <BrowserRouter>
      <Routes>
        {/* Rotas publicas (nao precisam de token) */}
        <Route path="/" element={<App />} />  {/*LOGIN*/}
        <Route path="/reset-password" element={<ResetPassword />} />

        {/* privadas (precisam de token) */}
        <Route
          element={
            <RequireAuth>
              <Layout />
            </RequireAuth>
          }
        >
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/my-activities" element={<MyActivities />} />
          <Route path="/events/:id" element={<EventDetails />} />
          <Route path="/chat" element={<ChatList />} />
          <Route path="/chat/:id" element={<ChatRoom />} />
          <Route path="/events/create" element={<CreateEvent />} />
          <Route path="/account" element={<Account />} />
          <Route path="/profile/:id" element={<UserProfile />} />
          <Route path="/change-password" element={<ChangePassword />} />
          <Route path="/change-email" element={<ChangeEmail />} />
          <Route path="/delete-account" element={<DeleteAccountPage />} />
          <Route path="/account/edit" element={<EditProfilePage />} />
          <Route path="/events/:id/edit" element={<EditEvent />} />
          <Route path="/leaderboard"     element={<Leaderboard />} />
        </Route>

        {/* fallback 404 -----*/}
        <Route path="*" element={<p>Not Found</p>} />
      </Routes>
    </BrowserRouter>
    </QueryClientProvider>
);