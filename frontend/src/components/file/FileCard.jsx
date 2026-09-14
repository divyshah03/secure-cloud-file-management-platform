import {
    Box,
    Card,
    CardBody,
    CardHeader,
    Heading,
    Text,
    IconButton,
    Tooltip,
    Flex,
    Badge,
    useColorModeValue,
    HStack,
    VStack
} from '@chakra-ui/react';
import { TimeIcon } from '@chakra-ui/icons';

const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
};

const getFileIcon = (contentType) => {
    if (!contentType) return '📄';
    if (contentType.startsWith('image/')) return '🖼️';
    if (contentType.startsWith('video/')) return '🎥';
    if (contentType.startsWith('audio/')) return '🎵';
    if (contentType.includes('pdf')) return '📕';
    if (contentType.includes('word')) return '📘';
    if (contentType.includes('excel') || contentType.includes('spreadsheet')) return '📗';
    if (contentType.includes('zip') || contentType.includes('rar')) return '📦';
    return '📄';
};

export default function FileCard({ file, onDelete, onDownload }) {
    const cardBg = useColorModeValue('white', 'gray.800');
    const borderColor = useColorModeValue('gray.200', 'gray.700');

    const handleDownload = () => {
        onDownload(file.id, file.originalFileName);
    };

    const handleDelete = () => {
        onDelete(file.id, file.originalFileName);
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'Unknown';
        try {
            const date = new Date(dateString);
            const now = new Date();
            const diffMs = now - date;
            const diffMins = Math.floor(diffMs / 60000);
            const diffHours = Math.floor(diffMs / 3600000);
            const diffDays = Math.floor(diffMs / 86400000);
            
            if (diffMins < 1) return 'just now';
            if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
            if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
            if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
            return date.toLocaleDateString();
        } catch (e) {
            return 'Unknown';
        }
    };

    const formattedDate = formatDate(file.createdAt);

    return (
        <Card
            bg={cardBg}
            borderWidth={1}
            borderColor={borderColor}
            _hover={{ boxShadow: 'lg', transform: 'translateY(-2px)' }}
            transition="all 0.2s"
        >
            <CardHeader>
                <Flex justify="space-between" align="start">
                    <HStack spacing={2} flex={1} minW={0}>
                        <Text fontSize="2xl">{getFileIcon(file.contentType)}</Text>
                        <VStack align="start" spacing={0} flex={1} minW={0}>
                            <Heading size="sm" noOfLines={1} title={file.originalFileName}>
                                {file.originalFileName}
                            </Heading>
                            <Text fontSize="xs" color="gray.500" noOfLines={1}>
                                {file.fileName}
                            </Text>
                        </VStack>
                    </HStack>
                    <HStack ml={2}>
                        <Tooltip label="Download">
                            <IconButton
                                icon={<DownloadIcon />}
                                size="sm"
                                colorScheme="blue"
                                variant="ghost"
                                onClick={handleDownload}
                                aria-label="Download file"
                            />
                        </Tooltip>
                        <Tooltip label="Delete">
                            <IconButton
                                icon={<DeleteIcon />}
                                size="sm"
                                colorScheme="red"
                                variant="ghost"
                                onClick={handleDelete}
                                aria-label="Delete file"
                            />
                        </Tooltip>
                    </HStack>
                </Flex>
            </CardHeader>
            <CardBody pt={0}>
                <VStack align="stretch" spacing={2}>
                    <Flex justify="space-between" align="center">
                        <Text fontSize="sm" color="gray.600">
                            {formatFileSize(file.fileSize || 0)}
                        </Text>
                        <Badge colorScheme="blue" fontSize="xs">
                            {file.contentType?.split('/')[0] || 'file'}
                        </Badge>
                    </Flex>
                    <Flex align="center" fontSize="xs" color="gray.500">
                        <TimeIcon mr={1} />
                        <Text>Uploaded {formattedDate}</Text>
                    </Flex>
                </VStack>
            </CardBody>
        </Card>
    );
}
