import { useEffect, useState } from 'react';
import {
    Box,
    Container,
    Heading,
    Spinner,
    Table,
    Thead,
    Tbody,
    Tr,
    Th,
    Td,
    Text,
    Flex,
    Button,
    Badge
} from '@chakra-ui/react';
import Sidebar from '../components/layout/Sidebar.jsx';
import { getMyActivity } from '../api/client.js';
import { errorNotification } from '../notification.js';

const actionColor = {
    UPLOAD: 'green',
    DOWNLOAD: 'blue',
    DELETE: 'red',
    PERMISSION_GRANT: 'purple',
    PERMISSION_REVOKE: 'orange',
    SHARE_LINK_CREATE: 'teal',
    SHARE_LINK_REVOKE: 'orange',
    SHARE_LINK_REDEEM: 'gray'
};

const Activity = () => {
    const [entries, setEntries] = useState([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    useEffect(() => {
        setLoading(true);
        getMyActivity(page, 20)
            .then((res) => {
                setEntries(res.data.content || []);
                setTotalPages(res.data.totalPages ?? 0);
            })
            .catch((err) => {
                errorNotification('Error', err.response?.data?.message || 'Failed to load activity');
            })
            .finally(() => setLoading(false));
    }, [page]);

    return (
        <Sidebar>
            <Container maxW="container.xl" py={8}>
                <Heading size="lg" mb={6}>My Activity</Heading>

                {loading ? (
                    <Flex justify="center" py={8}>
                        <Spinner />
                    </Flex>
                ) : entries.length === 0 ? (
                    <Box p={8} borderWidth={1} borderRadius="lg" textAlign="center">
                        <Text fontSize="xl">No activity yet</Text>
                    </Box>
                ) : (
                    <Box overflowX="auto">
                        <Table size="sm">
                            <Thead>
                                <Tr>
                                    <Th>Action</Th>
                                    <Th>File</Th>
                                    <Th>Details</Th>
                                    <Th>When</Th>
                                </Tr>
                            </Thead>
                            <Tbody>
                                {entries.map((e) => (
                                    <Tr key={e.id}>
                                        <Td>
                                            <Badge colorScheme={actionColor[e.action] || 'gray'}>
                                                {e.action}
                                            </Badge>
                                        </Td>
                                        <Td>{e.fileName || '—'}</Td>
                                        <Td>{e.metadata || '—'}</Td>
                                        <Td whiteSpace="nowrap">{new Date(e.createdAt).toLocaleString()}</Td>
                                    </Tr>
                                ))}
                            </Tbody>
                        </Table>
                    </Box>
                )}

                {totalPages > 1 && (
                    <Flex justify="center" align="center" gap={4} mt={8}>
                        <Button
                            onClick={() => setPage((p) => Math.max(0, p - 1))}
                            isDisabled={page === 0 || loading}
                        >
                            Previous
                        </Button>
                        <Text fontSize="sm">Page {page + 1} of {totalPages}</Text>
                        <Button
                            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                            isDisabled={page >= totalPages - 1 || loading}
                        >
                            Next
                        </Button>
                    </Flex>
                )}
            </Container>
        </Sidebar>
    );
};

export default Activity;
