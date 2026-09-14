import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const getAuthConfig = () => ({
    headers: {
        Authorization: `Bearer ${localStorage.getItem("access_token")}`
    }
});

// Authentication APIs
export const register = async (userData) => {
    try {
        return await axios.post(
            `${API_BASE_URL}/api/v1/auth/register`,
            userData
        );
    } catch (e) {
        throw e;
    }
};

export const login = async (credentials) => {
    try {
        const response = await axios.post(
            `${API_BASE_URL}/api/v1/auth/login`,
            credentials
        );
        // Extract token from response header
        const token = response.headers.authorization || response.data.token;
        if (token) {
            localStorage.setItem("access_token", token);
        }
        return response;
    } catch (e) {
        throw e;
    }
};

export const verifyEmail = async (token) => {
    try {
        return await axios.get(
            `${API_BASE_URL}/api/v1/auth/verify-email`,
            { params: { token } }
        );
    } catch (e) {
        throw e;
    }
};

export const verifyEmailPost = async (token) => {
    try {
        return await axios.post(
            `${API_BASE_URL}/api/v1/auth/verify-email`,
            { token }
        );
    } catch (e) {
        throw e;
    }
};

export const resendVerificationEmail = async (email) => {
    try {
        return await axios.post(
            `${API_BASE_URL}/api/v1/auth/resend-verification`,
            { email }
        );
    } catch (e) {
        throw e;
    }
};

// File Management APIs
export const uploadFile = async (file) => {
    try {
        const formData = new FormData();
        formData.append('file', file);
        
        return await axios.post(
            `${API_BASE_URL}/api/v1/files`,
            formData,
            {
                ...getAuthConfig(),
                'Content-Type': 'multipart/form-data'
            }
        );
    } catch (e) {
        throw e;
    }
};

export const getFiles = async (page = 0, size = 20, sortBy = 'createdAt', sortDir = 'DESC') => {
    try {
        return await axios.get(
            `${API_BASE_URL}/api/v1/files`,
            {
                ...getAuthConfig(),
                params: { page, size, sortBy, sortDir }
            }
        );
    } catch (e) {
        throw e;
    }
};

export const getAllFiles = async () => {
    try {
        return await axios.get(
            `${API_BASE_URL}/api/v1/files/all`,
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

export const getFileById = async (fileId) => {
    try {
        return await axios.get(
            `${API_BASE_URL}/api/v1/files/${fileId}`,
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

export const downloadFile = async (fileId) => {
    try {
        const response = await axios.get(
            `${API_BASE_URL}/api/v1/files/${fileId}/download`,
            {
                ...getAuthConfig(),
                responseType: 'blob'
            }
        );
        return response;
    } catch (e) {
        throw e;
    }
};

export const deleteFile = async (fileId) => {
    try {
        return await axios.delete(
            `${API_BASE_URL}/api/v1/files/${fileId}`,
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

export const getFileStats = async () => {
    try {
        return await axios.get(
            `${API_BASE_URL}/api/v1/files/stats`,
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

// Helper to download file and trigger browser download
export const downloadFileAsBlob = async (fileId, fileName) => {
    try {
        const response = await downloadFile(fileId);
        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', fileName);
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
    } catch (e) {
        throw e;
    }
};
