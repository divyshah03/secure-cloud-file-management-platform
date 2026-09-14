import React from 'react';
import ReactDOM from 'react-dom/client';
import { ChakraProvider } from '@chakra-ui/react';
import { createStandaloneToast } from '@chakra-ui/toast';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import Login from './pages/Login.jsx';
import Signup from './pages/Signup.jsx';
import EmailVerification from './pages/EmailVerification.jsx';
import AuthProvider from './AuthProvider.jsx';
import ProtectedRoute from './components/layout/ProtectedRoute.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Files from './pages/Files.jsx';

const { ToastContainer } = createStandaloneToast();

const router = createBrowserRouter([
    {
        path: '/',
        element: <Login />
    },
    {
        path: '/login',
        element: <Login />
    },
    {
        path: '/signup',
        element: <Signup />
    },
    {
        path: '/verify-email',
        element: <EmailVerification />
    },
    {
        path: '/dashboard',
        element: <ProtectedRoute><Dashboard /></ProtectedRoute>
    },
    {
        path: '/dashboard/files',
        element: <ProtectedRoute><Files /></ProtectedRoute>
    }
]);

ReactDOM
    .createRoot(document.getElementById('root'))
    .render(
        <React.StrictMode>
            <ChakraProvider>
                <AuthProvider>
                    <RouterProvider router={router} />
                </AuthProvider>
                <ToastContainer />
            </ChakraProvider>
        </React.StrictMode>
    );
