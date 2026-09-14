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
    AlertIcon,
    Select,
    HStack,
    AlertDialog,
    AlertDialogBody,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogContent,
    AlertDialogOverlay
} from '@chakra-ui/react';
import { AddIcon, RepeatIcon } from '@chakra-ui/icons';
import Sidebar from '../components/layout/Sidebar.jsx';
import { useEffect, useState, useRef } from 'react';
import { getFiles, deleteFile, downloadFileAsBlob, getFileStats } from '../api/client.js';
import FileCard from '../components/file/FileCard.jsx';
import FileUpload from '../components/file/FileUpload.jsx';
import { errorNotification, successNotification } from '../notification.js';

const Files = () => {
    const [files, setFiles] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [stats, setStats] = useState({ fileCount: 0, totalSize: 0, totalSizeMB: "0.00" });
    const { isOpen, onOpen, onClose } = useDisclosure();
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [sortBy, setSortBy] = useState('createdAt');
    const [sortDir, setSortDir] = useState('DESC');
    const {
        isOpen: isDeleteOpen,
        onOpen: onDeleteOpen,
        onClose: onDeleteClose
    } = useDisclosure();
    const [fileToDelete, setFileToDelete] = useState(null);
    const cancelRef = useRef();

    const fetchFiles = () => {
        setLoading(true);
        setError("");
        getFiles(page, 20, sortBy, sortDir)
            .then(res => {
                setFiles(res.data.content || res.data);
                setTotalPages(res.data.totalPages ?? 0);
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

    const handleDeleteFile = (fileId, fileName) => {
        setFileToDelete({ id: fileId, name: fileName });
        onDeleteOpen();
    };

    const confirmDeleteFile = async () => {
        if (!fileToDelete) return;
        try {
            await deleteFile(fileToDelete.id);
            successNotification("Success", "File deleted successfully");
            fetchFiles();
            fetchStats();
        } catch (err) {
            const errorMessage = err.response?.data?.message ||
                               err.response?.data?.error ||
                               err.message ||
                               "Failed to delete file";
            errorNotification("Error", errorMessage);
        } finally {
            onDeleteClose();
            setFileToDelete(null);
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
            <Sidebar>
                <Flex justify="center" align="center" minH="400px">
                    <Spinner
                        thickness='4px'
                        speed='0.65s'
                        emptyColor='gray.200'
                        color='blue.500'
                        size='xl'
                    />
                </Flex>
            </Sidebar>
        );
    }

    return (
        <Sidebar>
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

                <HStack spacing={4} mb={6} flexWrap="wrap">
                    <Select
                        maxW="220px"
                        value={sortBy}
                        onChange={(e) => {
                            setSortBy(e.target.value);
                            setPage(0);
                        }}
                        aria-label="Sort by"
                    >
                        <option value="createdAt">Sort by date</option>
                        <option value="originalFileName">Sort by name</option>
                        <option value="fileSize">Sort by size</option>
                    </Select>
                    <Select
                        maxW="160px"
                        value={sortDir}
                        onChange={(e) => {
                            setSortDir(e.target.value);
                            setPage(0);
                        }}
                        aria-label="Sort direction"
                    >
                        <option value="DESC">Descending</option>
                        <option value="ASC">Ascending</option>
                    </Select>
                </HStack>

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

                {totalPages > 1 && (
                    <Flex justify="center" align="center" gap={4} mt={8}>
                        <Button
                            onClick={() => setPage((p) => Math.max(0, p - 1))}
                            isDisabled={page === 0 || loading}
                        >
                            Previous
                        </Button>
                        <Text fontSize="sm">
                            Page {page + 1} of {totalPages}
                        </Text>
                        <Button
                            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                            isDisabled={page >= totalPages - 1 || loading}
                        >
                            Next
                        </Button>
                    </Flex>
                )}

                <FileUpload
                    isOpen={isOpen}
                    onClose={onClose}
                    onSuccess={handleFileUploaded}
                />

                <AlertDialog
                    isOpen={isDeleteOpen}
                    leastDestructiveRef={cancelRef}
                    onClose={onDeleteClose}
                >
                    <AlertDialogOverlay>
                        <AlertDialogContent>
                            <AlertDialogHeader fontSize="lg" fontWeight="bold">
                                Delete File
                            </AlertDialogHeader>
                            <AlertDialogBody>
                                Are you sure you want to delete "{fileToDelete?.name}"? This cannot be undone.
                            </AlertDialogBody>
                            <AlertDialogFooter>
                                <Button ref={cancelRef} onClick={onDeleteClose}>
                                    Cancel
                                </Button>
                                <Button colorScheme="red" onClick={confirmDeleteFile} ml={3}>
                                    Delete
                                </Button>
                            </AlertDialogFooter>
                        </AlertDialogContent>
                    </AlertDialogOverlay>
                </AlertDialog>
            </Container>
        </Sidebar>
    );
};

export default Files;
