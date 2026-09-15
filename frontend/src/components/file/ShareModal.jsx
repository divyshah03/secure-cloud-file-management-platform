import { useState, useEffect, useCallback } from 'react';
import {
    Modal,
    ModalOverlay,
    ModalContent,
    ModalHeader,
    ModalFooter,
    ModalBody,
    ModalCloseButton,
    Button,
    Box,
    Text,
    VStack,
    HStack,
    Input,
    Select,
    IconButton,
    Divider,
    Heading,
    Badge,
    Tooltip,
    Spinner,
    NumberInput,
    NumberInputField
} from '@chakra-ui/react';
import { FiTrash2, FiCopy, FiUserPlus, FiLink } from 'react-icons/fi';
import {
    listFilePermissions,
    grantFilePermission,
    revokeFilePermission,
    listShareLinks,
    createShareLink,
    revokeShareLink,
    getFileAuditLog
} from '../../api/client.js';
import { errorNotification, successNotification } from '../../notification.js';

export default function ShareModal({ isOpen, onClose, file }) {
    const [collaborators, setCollaborators] = useState([]);
    const [links, setLinks] = useState([]);
    const [activity, setActivity] = useState([]);
    const [loading, setLoading] = useState(false);

    const [email, setEmail] = useState('');
    const [role, setRole] = useState('VIEWER');
    const [granting, setGranting] = useState(false);

    const [linkRole, setLinkRole] = useState('VIEWER');
    const [expiresInHours, setExpiresInHours] = useState(24);
    const [creatingLink, setCreatingLink] = useState(false);

    const fileId = file?.id;

    const loadData = useCallback(() => {
        if (!fileId) return;
        setLoading(true);
        Promise.all([listFilePermissions(fileId), listShareLinks(fileId), getFileAuditLog(fileId, 0, 10)])
            .then(([permRes, linkRes, auditRes]) => {
                setCollaborators(permRes.data);
                setLinks(linkRes.data);
                setActivity(auditRes.data.content || []);
            })
            .catch((err) => {
                const msg = err.response?.data?.message || err.message || 'Failed to load sharing info';
                errorNotification('Error', msg);
            })
            .finally(() => setLoading(false));
    }, [fileId]);

    useEffect(() => {
        if (isOpen) loadData();
    }, [isOpen, loadData]);

    const handleGrant = async () => {
        if (!email) {
            errorNotification('Error', 'Please enter an email address');
            return;
        }
        setGranting(true);
        try {
            await grantFilePermission(fileId, email, role);
            successNotification('Success', `Access granted to ${email}`);
            setEmail('');
            loadData();
        } catch (err) {
            const msg = err.response?.data?.message || err.message || 'Failed to grant access';
            errorNotification('Error', msg);
        } finally {
            setGranting(false);
        }
    };

    const handleRevoke = async (userId, userEmail) => {
        try {
            await revokeFilePermission(fileId, userId);
            successNotification('Success', `Access revoked for ${userEmail}`);
            loadData();
        } catch (err) {
            const msg = err.response?.data?.message || err.message || 'Failed to revoke access';
            errorNotification('Error', msg);
        }
    };

    const handleCreateLink = async () => {
        setCreatingLink(true);
        try {
            const res = await createShareLink(fileId, linkRole, Number(expiresInHours));
            successNotification('Success', 'Share link created');
            if (navigator.clipboard) {
                navigator.clipboard.writeText(res.data.url).catch(() => {});
            }
            loadData();
        } catch (err) {
            const msg = err.response?.data?.message || err.message || 'Failed to create share link';
            errorNotification('Error', msg);
        } finally {
            setCreatingLink(false);
        }
    };

    const handleCopyLink = (url) => {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(url);
            successNotification('Copied', 'Link copied to clipboard');
        }
    };

    const handleRevokeLink = async (linkId) => {
        try {
            await revokeShareLink(fileId, linkId);
            successNotification('Success', 'Share link revoked');
            loadData();
        } catch (err) {
            const msg = err.response?.data?.message || err.message || 'Failed to revoke share link';
            errorNotification('Error', msg);
        }
    };

    return (
        <Modal isOpen={isOpen} onClose={onClose} size="lg">
            <ModalOverlay />
            <ModalContent>
                <ModalHeader>Share "{file?.originalFileName}"</ModalHeader>
                <ModalCloseButton />
                <ModalBody>
                    {loading ? (
                        <Box textAlign="center" py={8}>
                            <Spinner />
                        </Box>
                    ) : (
                        <VStack align="stretch" spacing={6}>
                            <Box>
                                <Heading size="sm" mb={3}>People with access</Heading>
                                <HStack mb={3}>
                                    <Input
                                        placeholder="Email address"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                    />
                                    <Select maxW="130px" value={role} onChange={(e) => setRole(e.target.value)}>
                                        <option value="VIEWER">Viewer</option>
                                        <option value="EDITOR">Editor</option>
                                    </Select>
                                    <IconButton
                                        icon={<FiUserPlus />}
                                        colorScheme="blue"
                                        aria-label="Grant access"
                                        isLoading={granting}
                                        onClick={handleGrant}
                                    />
                                </HStack>
                                <VStack align="stretch" spacing={2}>
                                    {collaborators.length === 0 ? (
                                        <Text fontSize="sm" color="gray.500">
                                            No collaborators yet
                                        </Text>
                                    ) : (
                                        collaborators.map((c) => (
                                            <HStack key={c.id} justify="space-between" p={2} borderWidth={1} borderRadius="md">
                                                <Box>
                                                    <Text fontSize="sm" fontWeight="semibold">{c.userName}</Text>
                                                    <Text fontSize="xs" color="gray.500">{c.userEmail}</Text>
                                                </Box>
                                                <HStack>
                                                    <Badge colorScheme={c.role === 'EDITOR' ? 'purple' : 'gray'}>
                                                        {c.role}
                                                    </Badge>
                                                    <IconButton
                                                        icon={<FiTrash2 />}
                                                        size="sm"
                                                        variant="ghost"
                                                        colorScheme="red"
                                                        aria-label="Revoke access"
                                                        onClick={() => handleRevoke(c.userId, c.userEmail)}
                                                    />
                                                </HStack>
                                            </HStack>
                                        ))
                                    )}
                                </VStack>
                            </Box>

                            <Divider />

                            <Box>
                                <Heading size="sm" mb={3}>Shareable links</Heading>
                                <HStack mb={3} align="flex-end">
                                    <Box flex={1}>
                                        <Text fontSize="xs" mb={1}>Access level</Text>
                                        <Select value={linkRole} onChange={(e) => setLinkRole(e.target.value)}>
                                            <option value="VIEWER">Viewer</option>
                                            <option value="EDITOR">Editor</option>
                                        </Select>
                                    </Box>
                                    <Box flex={1}>
                                        <Text fontSize="xs" mb={1}>Expires in (hours)</Text>
                                        <NumberInput
                                            min={1}
                                            max={8760}
                                            value={expiresInHours}
                                            onChange={(val) => setExpiresInHours(val)}
                                        >
                                            <NumberInputField />
                                        </NumberInput>
                                    </Box>
                                    <IconButton
                                        icon={<FiLink />}
                                        colorScheme="blue"
                                        aria-label="Create share link"
                                        isLoading={creatingLink}
                                        onClick={handleCreateLink}
                                    />
                                </HStack>
                                <VStack align="stretch" spacing={2}>
                                    {links.length === 0 ? (
                                        <Text fontSize="sm" color="gray.500">
                                            No share links yet
                                        </Text>
                                    ) : (
                                        links.map((l) => (
                                            <HStack key={l.id} justify="space-between" p={2} borderWidth={1} borderRadius="md">
                                                <Box minW={0} flex={1}>
                                                    <Text fontSize="sm" noOfLines={1}>{l.url}</Text>
                                                    <HStack>
                                                        <Badge colorScheme={l.role === 'EDITOR' ? 'purple' : 'gray'}>
                                                            {l.role}
                                                        </Badge>
                                                        {l.revoked ? (
                                                            <Badge colorScheme="red">Revoked</Badge>
                                                        ) : l.expired ? (
                                                            <Badge colorScheme="orange">Expired</Badge>
                                                        ) : (
                                                            <Badge colorScheme="green">Active</Badge>
                                                        )}
                                                    </HStack>
                                                </Box>
                                                <HStack>
                                                    <Tooltip label="Copy link">
                                                        <IconButton
                                                            icon={<FiCopy />}
                                                            size="sm"
                                                            variant="ghost"
                                                            aria-label="Copy link"
                                                            onClick={() => handleCopyLink(l.url)}
                                                        />
                                                    </Tooltip>
                                                    {!l.revoked && (
                                                        <IconButton
                                                            icon={<FiTrash2 />}
                                                            size="sm"
                                                            variant="ghost"
                                                            colorScheme="red"
                                                            aria-label="Revoke link"
                                                            onClick={() => handleRevokeLink(l.id)}
                                                        />
                                                    )}
                                                </HStack>
                                            </HStack>
                                        ))
                                    )}
                                </VStack>
                            </Box>

                            <Divider />

                            <Box>
                                <Heading size="sm" mb={3}>Recent activity</Heading>
                                <VStack align="stretch" spacing={1}>
                                    {activity.length === 0 ? (
                                        <Text fontSize="sm" color="gray.500">
                                            No activity recorded yet
                                        </Text>
                                    ) : (
                                        activity.map((a) => (
                                            <HStack key={a.id} justify="space-between" fontSize="xs" color="gray.600">
                                                <Text>
                                                    {a.action} — {a.actorEmail || 'anonymous (via share link)'}
                                                    {a.metadata ? ` (${a.metadata})` : ''}
                                                </Text>
                                                <Text whiteSpace="nowrap">
                                                    {new Date(a.createdAt).toLocaleString()}
                                                </Text>
                                            </HStack>
                                        ))
                                    )}
                                </VStack>
                            </Box>
                        </VStack>
                    )}
                </ModalBody>
                <ModalFooter>
                    <Button onClick={onClose}>Close</Button>
                </ModalFooter>
            </ModalContent>
        </Modal>
    );
}
