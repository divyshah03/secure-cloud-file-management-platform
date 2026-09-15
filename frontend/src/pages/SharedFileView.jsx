import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
    Container,
    Box,
    Heading,
    Text,
    Button,
    Spinner,
    Alert,
    AlertIcon,
    AlertTitle,
    AlertDescription,
    VStack,
    Badge
} from '@chakra-ui/react';
import { DownloadIcon } from '@chakra-ui/icons';
import { getSharedFileInfo, downloadSharedFileDirect } from '../api/client.js';
import { formatFileSize } from '../utils/formatFileSize.js';
import { errorNotification, successNotification } from '../notification.js';

const SharedFileView = () => {
    const { token } = useParams();
    const [file, setFile] = useState(null);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(true);
    const [downloading, setDownloading] = useState(false);

    useEffect(() => {
        getSharedFileInfo(token)
            .then((res) => setFile(res.data))
            .catch((err) => {
                const msg = err.response?.data?.message || 'This link is invalid, expired, or has been revoked.';
                setError(msg);
            })
            .finally(() => setLoading(false));
    }, [token]);

    const handleDownload = async () => {
        setDownloading(true);
        try {
            await downloadSharedFileDirect(token, file.originalFileName);
            successNotification('Success', 'Download started');
        } catch (err) {
            errorNotification('Error', 'Failed to download file');
        } finally {
            setDownloading(false);
        }
    };

    if (loading) {
        return (
            <Container maxW="md" centerContent mt={20}>
                <Spinner size="xl" />
            </Container>
        );
    }

    if (error) {
        return (
            <Container maxW="md" centerContent mt={20}>
                <Alert status="error" borderRadius="md">
                    <AlertIcon />
                    <Box>
                        <AlertTitle>Link unavailable</AlertTitle>
                        <AlertDescription>{error}</AlertDescription>
                    </Box>
                </Alert>
            </Container>
        );
    }

    return (
        <Container maxW="md" centerContent mt={20}>
            <Box p={8} borderWidth={1} borderRadius="lg" boxShadow="lg" w="full">
                <VStack align="stretch" spacing={4}>
                    <Heading size="lg">Shared File</Heading>
                    <Box>
                        <Text fontWeight="semibold" fontSize="lg">{file.originalFileName}</Text>
                        <Text fontSize="sm" color="gray.500">{formatFileSize(file.fileSize)}</Text>
                        <Badge mt={2} colorScheme={file.role === 'EDITOR' ? 'purple' : 'gray'}>
                            {file.role} access
                        </Badge>
                    </Box>
                    <Button
                        leftIcon={<DownloadIcon />}
                        colorScheme="blue"
                        onClick={handleDownload}
                        isLoading={downloading}
                    >
                        Download
                    </Button>
                </VStack>
            </Box>
        </Container>
    );
};

export default SharedFileView;
