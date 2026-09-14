import {
    Box,
    Button,
    Container,
    Flex,
    Heading,
    SimpleGrid,
    Spinner,
    Text,
    Stat,
    StatLabel,
    StatNumber,
    StatHelpText,
    useDisclosure,
    IconButton,
    Tooltip,
    Alert,
    AlertIcon
} from '@chakra-ui/react';
import { AddIcon, RepeatIcon } from '@chakra-ui/icons';
import SidebarWithHeader from '../../components/layout/SideBar.jsx';
import { useEffect, useState } from 'react';
import { getFiles, deleteFile, downloadFileAsBlob, getFileStats } from '../../api/client.js';
import FileCard from '../../components/file/FileCard.jsx';
import FileUpload from '../../components/file/FileUpload.jsx';
import { errorNotification, successNotification } from '../../utils/notification.js';

const Files = () => {
    const [files, setFiles] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [stats, setStats] = useState({ fileCount: 0, totalSize: 0, totalSizeMB: "0.00" });
    const { isOpen, onOpen, onClose } = useDisclosure();
    const [page, setPage] = useState(0);
    const [sortBy, setSortBy] = useState('createdAt');
    const [sortDir, setSortDir] = useState('DESC');

    const fetchFiles = () => {
        setLoading(true);
        setError("");
        getFiles(page, 20, sortBy, sortDir)
            .then(res => {
                setFiles(res.data.content || res.data);
            })
            .catch(err => {
                const errorMessage = err.response?.data?.message || 
                                   err.response?.data?.error || 
                                   err.message || 
                                   "Failed to load files";
                setError(errorMessage);
                errorNotification("Error", errorMessage);
            })
            .finally(() => {
                setLoading(false);
            });
    };

    const fetchStats = () => {
        getFileStats()
            .then(res => {
                setStats({
                    fileCount: res.data.fileCount || 0,
                    totalSize: res.data.totalSize || 0,
                    totalSizeMB: res.data.totalSizeMB || "0.00"
                });
            })
            .catch(err => {
                console.error("Failed to load stats:", err);
            });
    };

    useEffect(() => {
        fetchFiles();
        fetchStats();
    }, [page, sortBy, sortDir]);

    const handleFileUploaded = () => {
        onClose();
        fetchFiles();
        fetchStats();
        successNotification("Success", "File uploaded successfully");
    };

    const handleDeleteFile = async (fileId, fileName) => {
        if (window.confirm(`Are you sure you want to delete "${fileName}"?`)) {
            try {
                await deleteFile(fileId);
                successNotification("Success", "File deleted successfully");
                fetchFiles();
                fetchStats();
            } catch (err) {
                const errorMessage = err.response?.data?.message || 
                                   err.response?.data?.error || 
                                   err.message || 
                                   "Failed to delete file";
                errorNotification("Error", errorMessage);
            }
        }
    };

    const handleDownloadFile = async (fileId, fileName) => {
        try {
            await downloadFileAsBlob(fileId, fileName);
            successNotification("Success", "File download started");
        } catch (err) {
            const errorMessage = err.response?.data?.message || 
                               err.response?.data?.error || 
                               err.message || 
                               "Failed to download file";
            errorNotification("Error", errorMessage);
        }
    };

    if (loading && files.length === 0) {
        return (
            <SidebarWithHeader>
                <Flex justify="center" align="center" minH="400px">
                    <Spinner
                        thickness='4px'
                        speed='0.65s'
                        emptyColor='gray.200'
                        color='blue.500'
                        size='xl'
                    />
                </Flex>
            </SidebarWithHeader>
        );
    }

    return (
        <SidebarWithHeader>
            <Container maxW="container.xl" py={8}>
                <Flex justify="space-between" align="center" mb={6}>
                    <Heading size="lg">My Files</Heading>
                    <Flex gap={2}>
                        <Tooltip label="Refresh">
                            <IconButton
                                icon={<RepeatIcon />}
                                onClick={() => {
                                    fetchFiles();
                                    fetchStats();
                                }}
                                aria-label="Refresh"
                            />
                        </Tooltip>
                        <Button
                            leftIcon={<AddIcon />}
                            colorScheme="blue"
                            onClick={onOpen}
                        >
                            Upload File
                        </Button>
                    </Flex>
                </Flex>

                {/* Statistics */}
                <SimpleGrid columns={{ base: 1, md: 3 }} spacing={4} mb={6}>
                    <Stat>
                        <StatLabel>Total Files</StatLabel>
                        <StatNumber>{stats.fileCount}</StatNumber>
                    </Stat>
                    <Stat>
                        <StatLabel>Total Size</StatLabel>
                        <StatNumber>{stats.totalSizeMB} MB</StatNumber>
                        <StatHelpText>{stats.totalSize.toLocaleString()} bytes</StatHelpText>
                    </Stat>
                    <Stat>
                        <StatLabel>Files Shown</StatLabel>
                        <StatNumber>{files.length}</StatNumber>
                    </Stat>
                </SimpleGrid>

                {error && (
                    <Alert status="error" mb={4} borderRadius="md">
                        <AlertIcon />
                        {error}
                    </Alert>
                )}

                {files.length === 0 && !loading ? (
                    <Box
                        p={8}
                        borderWidth={1}
                        borderRadius="lg"
                        textAlign="center"
                    >
                        <Text fontSize="xl" mb={4}>No files uploaded yet</Text>
                        <Button
                            leftIcon={<AddIcon />}
                            colorScheme="blue"
                            onClick={onOpen}
                        >
                            Upload Your First File
                        </Button>
                    </Box>
                ) : (
                    <SimpleGrid columns={{ base: 1, md: 2, lg: 3 }} spacing={4}>
                        {files.map((file) => (
                            <FileCard
                                key={file.id}
                                file={file}
                                onDelete={handleDeleteFile}
                                onDownload={handleDownloadFile}
                            />
                        ))}
                    </SimpleGrid>
                )}

                {/* File Upload Modal */}
                <FileUpload
                    isOpen={isOpen}
                    onClose={onClose}
                    onSuccess={handleFileUploaded}
                />
            </Container>
        </SidebarWithHeader>
    );
};

export default Files;
