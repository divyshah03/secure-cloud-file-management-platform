import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const api = axios.create({
    baseURL: API_BASE_URL
});

api.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error.response?.status;
        const url = error.config?.url || '';
        const isAuthEndpoint = url.includes('/api/v1/auth/');

        if (status === 401 && !isAuthEndpoint) {
            localStorage.removeItem('access_token');
            if (window.location.pathname !== '/login') {
                window.location.assign('/login');
            }
        }
        return Promise.reject(error);
    }
);

const getAuthConfig = () => ({
    headers: {
        Authorization: `Bearer ${localStorage.getItem("access_token")}`
    }
});

// Authentication APIs
export const register = async (userData) => {
    try {
        return await api.post(
            `/api/v1/auth/register`,
            userData
        );
    } catch (e) {
        throw e;
    }
};

export const login = async (credentials) => {
    try {
        const response = await api.post(
            `/api/v1/auth/login`,
            credentials
        );
        // Use the raw token from the body; the Authorization response header
        // carries a "Bearer " prefix and would double up if read here too.
        const token = response.data.token;
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
        return await api.get(
            `/api/v1/auth/verify-email`,
            { params: { token } }
        );
    } catch (e) {
        throw e;
    }
};

export const verifyEmailPost = async (token) => {
    try {
        return await api.post(
            `/api/v1/auth/verify-email`,
            { token }
        );
    } catch (e) {
        throw e;
    }
};

export const resendVerificationEmail = async (email) => {
    try {
        return await api.post(
            `/api/v1/auth/resend-verification`,
            { email }
        );
    } catch (e) {
        throw e;
    }
};

// Presigned S3 URL flow (direct browser <-> S3, bypassing the backend for bytes)
export const createPresignedUpload = async (originalFileName, contentType, fileSize) => {
    return await api.post(
        `/api/v1/files/presigned-upload`,
        { originalFileName, contentType, fileSize },
        getAuthConfig()
    );
};

export const putToPresignedUrl = async (uploadUrl, file, onUploadProgress) => {
    // Deliberately NOT using the `api` axios instance: this goes straight to S3/MinIO,
    // not our backend, so no baseURL and no Authorization header belong here.
    return await axios.put(uploadUrl, file, {
        headers: { 'Content-Type': file.type || 'application/octet-stream' },
        onUploadProgress: onUploadProgress
            ? (event) => {
                if (event.total) {
                    onUploadProgress(Math.round((event.loaded * 100) / event.total));
                }
            }
            : undefined
    });
};

export const completePresignedUpload = async (s3Key, originalFileName, contentType, fileSize) => {
    return await api.post(
        `/api/v1/files/presigned-upload/complete`,
        { s3Key, originalFileName, contentType, fileSize },
        getAuthConfig()
    );
};

export const getPresignedDownloadUrl = async (fileId) => {
    return await api.get(
        `/api/v1/files/${fileId}/presigned-download`,
        getAuthConfig()
    );
};

export const getPresignedSharedDownloadUrl = async (token) => {
    return await api.get(`/api/v1/share/${token}/presigned-download`);
};

// File Management APIs
export const uploadFile = async (file, onUploadProgress) => {
    try {
        const formData = new FormData();
        formData.append('file', file);

        return await api.post(
            `/api/v1/files`,
            formData,
            {
                ...getAuthConfig(),
                headers: {
                    ...getAuthConfig().headers,
                    'Content-Type': 'multipart/form-data'
                },
                onUploadProgress: onUploadProgress
                    ? (event) => {
                        if (event.total) {
                            onUploadProgress(Math.round((event.loaded * 100) / event.total));
                        }
                    }
                    : undefined
            }
        );
    } catch (e) {
        throw e;
    }
};

// Uploads a file straight to S3/MinIO via a presigned URL, falling back to the
// backend-proxied upload if presigned URLs aren't available (e.g. AWS_S3_MOCK=true).
export const uploadFileDirect = async (file, onUploadProgress) => {
    let presign;
    try {
        presign = await createPresignedUpload(file.name, file.type || 'application/octet-stream', file.size);
    } catch (e) {
        // Presigned URLs unavailable in this environment - fall back to the proxied path.
        return uploadFile(file, onUploadProgress);
    }

    const { uploadUrl, s3Key } = presign.data;
    try {
        await putToPresignedUrl(uploadUrl, file, onUploadProgress);
    } catch (e) {
        const err = new Error('Direct upload to storage failed. Please check your connection and try again.');
        err.cause = e;
        throw err;
    }

    return completePresignedUpload(s3Key, file.name, file.type || 'application/octet-stream', file.size);
};

// Downloads a file by redirecting the browser straight to a presigned S3/MinIO URL,
// falling back to the backend-proxied download if presigned URLs aren't available.
export const downloadFileDirect = async (fileId, fileName) => {
    try {
        const res = await getPresignedDownloadUrl(fileId);
        triggerBrowserDownload(res.data.downloadUrl);
    } catch (e) {
        await downloadFileAsBlob(fileId, fileName);
    }
};

export const downloadSharedFileDirect = async (token, fileName) => {
    try {
        const res = await getPresignedSharedDownloadUrl(token);
        triggerBrowserDownload(res.data.downloadUrl);
    } catch (e) {
        await downloadSharedFile(token, fileName);
    }
};

const triggerBrowserDownload = (url) => {
    const link = document.createElement('a');
    link.href = url;
    document.body.appendChild(link);
    link.click();
    link.remove();
};

export const getFiles = async (page = 0, size = 20, sortBy = 'createdAt', sortDir = 'DESC') => {
    try {
        return await api.get(
            `/api/v1/files`,
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
        return await api.get(
            `/api/v1/files/all`,
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

export const getSharedWithMeFiles = async () => {
    try {
        return await api.get(
            `/api/v1/files/shared-with-me`,
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

export const getFileById = async (fileId) => {
    try {
        return await api.get(
            `/api/v1/files/${fileId}`,
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

export const downloadFile = async (fileId) => {
    try {
        const response = await api.get(
            `/api/v1/files/${fileId}/download`,
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
        return await api.delete(
            `/api/v1/files/${fileId}`,
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

export const getFileStats = async () => {
    try {
        return await api.get(
            `/api/v1/files/stats`,
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

// Collaborator permission APIs
export const listFilePermissions = async (fileId) => {
    try {
        return await api.get(
            `/api/v1/files/${fileId}/permissions`,
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

export const grantFilePermission = async (fileId, email, role) => {
    try {
        return await api.post(
            `/api/v1/files/${fileId}/permissions`,
            { email, role },
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

export const revokeFilePermission = async (fileId, userId) => {
    try {
        return await api.delete(
            `/api/v1/files/${fileId}/permissions/${userId}`,
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

// Shareable link APIs
export const listShareLinks = async (fileId) => {
    try {
        return await api.get(
            `/api/v1/files/${fileId}/share-links`,
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

export const createShareLink = async (fileId, role, expiresInHours) => {
    try {
        return await api.post(
            `/api/v1/files/${fileId}/share-links`,
            { role, expiresInHours },
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

export const revokeShareLink = async (fileId, linkId) => {
    try {
        return await api.delete(
            `/api/v1/files/${fileId}/share-links/${linkId}`,
            getAuthConfig()
        );
    } catch (e) {
        throw e;
    }
};

// Audit log / activity feed APIs
export const getFileAuditLog = async (fileId, page = 0, size = 20) => {
    return await api.get(
        `/api/v1/files/${fileId}/audit-log`,
        { ...getAuthConfig(), params: { page, size } }
    );
};

export const getMyActivity = async (page = 0, size = 20) => {
    return await api.get(
        `/api/v1/audit-log/me`,
        { ...getAuthConfig(), params: { page, size } }
    );
};

// Public share-link redemption APIs (no auth required)
export const getSharedFileInfo = async (token) => {
    try {
        return await api.get(`/api/v1/share/${token}`);
    } catch (e) {
        throw e;
    }
};

export const downloadSharedFile = async (token, fileName) => {
    try {
        const response = await api.get(`/api/v1/share/${token}/download`, {
            responseType: 'blob'
        });
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
