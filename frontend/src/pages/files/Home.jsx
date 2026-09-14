import {
    Box,
    Container,
    SimpleGrid,
    Stat,
    StatLabel,
    StatNumber,
    StatHelpText,
    Heading,
    Button,
    Text,
    Flex,
    useDisclosure
} from '@chakra-ui/react';
import SidebarWithHeader from '../../components/layout/SideBar.jsx';
import { useEffect, useState } from 'react';
import { getFileStats, getFiles } from '../../api/client.js';
import { errorNotification } from '../../utils/notification.js';
import { useNavigate } from 'react-router-dom';
import { AddIcon } from '@chakra-ui/icons';

const Home = () => {
    const [stats, setStats] = useState({ fileCount: 0, totalSize: 0, totalSizeMB: "0.00" });
    const [recentFiles, setRecentFiles] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        fetchStats();
        fetchRecentFiles();
    }, []);

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
                errorNotification("Error", "Failed to load statistics");
            })
            .finally(() => {
                setLoading(false);
            });
    };

    const fetchRecentFiles = () => {
        getFiles(0, 5, 'createdAt', 'DESC')
            .then(res => {
                const files = res.data.content || res.data || [];
                setRecentFiles(Array.isArray(files) ? files.slice(0, 5) : []);
            })
            .catch(err => {
                console.error("Failed to load recent files:", err);
            });
    };

    return (
        <SidebarWithHeader>
            <Container maxW="container.xl" py={8}>
                <Flex justify="space-between" align="center" mb={6}>
                    <Heading size="lg">Dashboard</Heading>
                    <Button
                        leftIcon={<AddIcon />}
                        colorScheme="blue"
                        onClick={() => navigate('/dashboard/files')}
                    >
                        Upload Files
                    </Button>
                </Flex>

                {/* Statistics */}
                <SimpleGrid columns={{ base: 1, md: 3 }} spacing={6} mb={8}>
                    <Stat
                        p={6}
                        borderWidth={1}
                        borderRadius="lg"
                        boxShadow="md"
                        bg="white"
                    >
                        <StatLabel>Total Files</StatLabel>
                        <StatNumber>{stats.fileCount}</StatNumber>
                        <StatHelpText>All uploaded files</StatHelpText>
                    </Stat>
                    <Stat
                        p={6}
                        borderWidth={1}
                        borderRadius="lg"
                        boxShadow="md"
                        bg="white"
                    >
                        <StatLabel>Total Storage Used</StatLabel>
                        <StatNumber>{stats.totalSizeMB} MB</StatNumber>
                        <StatHelpText>{stats.totalSize.toLocaleString()} bytes</StatHelpText>
                    </Stat>
                    <Stat
                        p={6}
                        borderWidth={1}
                        borderRadius="lg"
                        boxShadow="md"
                        bg="white"
                    >
                        <StatLabel>Recent Files</StatLabel>
                        <StatNumber>{recentFiles.length}</StatNumber>
                        <StatHelpText>Last 5 uploaded</StatHelpText>
                    </Stat>
                </SimpleGrid>

                {/* Quick Actions */}
                <Box
                    p={6}
                    borderWidth={1}
                    borderRadius="lg"
                    boxShadow="md"
                    bg="white"
                    mb={6}
                >
                    <Heading size="md" mb={4}>Quick Actions</Heading>
                    <SimpleGrid columns={{ base: 1, md: 2 }} spacing={4}>
                        <Button
                            colorScheme="blue"
                            variant="outline"
                            onClick={() => navigate('/dashboard/files')}
                            size="lg"
                        >
                            View All Files
                        </Button>
                        <Button
                            colorScheme="green"
                            variant="outline"
                            onClick={() => navigate('/dashboard/files')}
                            size="lg"
                            leftIcon={<AddIcon />}
                        >
                            Upload New File
                        </Button>
                    </SimpleGrid>
                </Box>

                {/* Recent Files */}
                {recentFiles.length > 0 && (
                    <Box
                        p={6}
                        borderWidth={1}
                        borderRadius="lg"
                        boxShadow="md"
                        bg="white"
                    >
                        <Heading size="md" mb={4}>Recent Files</Heading>
                        {recentFiles.map((file) => (
                            <Box
                                key={file.id}
                                p={4}
                                borderWidth={1}
                                borderRadius="md"
                                mb={2}
                                _hover={{ bg: 'gray.50', cursor: 'pointer' }}
                                onClick={() => navigate('/dashboard/files')}
                            >
                                <Flex justify="space-between" align="center">
                                    <Box>
                                        <Text fontWeight="semibold">{file.originalFileName}</Text>
                                        <Text fontSize="sm" color="gray.500">
                                            {(file.fileSize / (1024 * 1024)).toFixed(2)} MB • {file.contentType}
                                        </Text>
                                    </Box>
                                </Flex>
                            </Box>
                        ))}
                        {recentFiles.length >= 5 && (
                            <Button
                                mt={4}
                                variant="link"
                                onClick={() => navigate('/dashboard/files')}
                            >
                                View All Files →
                            </Button>
                        )}
                    </Box>
                )}
            </Container>
        </SidebarWithHeader>
    );
};

export default Home;
