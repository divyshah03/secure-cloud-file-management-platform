import { useState, useCallback } from 'react';
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
    useToast,
    Progress,
    Alert,
    AlertIcon,
    Icon,
    Flex
} from '@chakra-ui/react';
import { useDropzone } from 'react-dropzone';
import { uploadFile } from '../../api/client.js';
import { errorNotification, successNotification } from '../../utils/notification.js';
import { FiUpload, FiFile, FiX } from 'react-icons/fi';

const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
};

export default function FileUpload({ isOpen, onClose, onSuccess }) {
    const [selectedFile, setSelectedFile] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const toast = useToast();

    const onDrop = useCallback((acceptedFiles, rejectedFiles) => {
        if (rejectedFiles.length > 0) {
            const rejection = rejectedFiles[0];
            if (rejection.errors.find(e => e.code === 'file-too-large')) {
                errorNotification("File Too Large", `File size must be less than ${formatFileSize(MAX_FILE_SIZE)}`);
            } else {
                errorNotification("Invalid File", rejection.errors[0].message);
            }
            return;
        }

        if (acceptedFiles.length > 0) {
            const file = acceptedFiles[0];
            if (file.size > MAX_FILE_SIZE) {
                errorNotification("File Too Large", `File size must be less than ${formatFileSize(MAX_FILE_SIZE)}`);
                return;
            }
            setSelectedFile(file);
        }
    }, []);

    const { getRootProps, getInputProps, isDragActive } = useDropzone({
        onDrop,
        maxSize: MAX_FILE_SIZE,
        multiple: false
    });

    const handleUpload = async () => {
        if (!selectedFile) {
            errorNotification("Error", "Please select a file to upload");
            return;
        }

        setUploading(true);
        setUploadProgress(0);

        try {
            // Simulate progress (since we can't track actual upload progress easily with axios)
            const progressInterval = setInterval(() => {
                setUploadProgress(prev => {
                    if (prev >= 90) {
                        clearInterval(progressInterval);
                        return 90;
                    }
                    return prev + 10;
                });
            }, 200);

            await uploadFile(selectedFile);
            
            clearInterval(progressInterval);
            setUploadProgress(100);
            
            successNotification("Success", "File uploaded successfully!");
            
            setTimeout(() => {
                handleClose();
                if (onSuccess) onSuccess();
            }, 500);
        } catch (err) {
            clearInterval(progressInterval);
            const errorMessage = err.response?.data?.message || 
                               err.response?.data?.error || 
                               err.message || 
                               "Failed to upload file";
            errorNotification("Upload Failed", errorMessage);
        } finally {
            setUploading(false);
            setUploadProgress(0);
        }
    };

    const handleClose = () => {
        setSelectedFile(null);
        setUploadProgress(0);
        setUploading(false);
        onClose();
    };

    const removeFile = () => {
        setSelectedFile(null);
    };

    return (
        <Modal isOpen={isOpen} onClose={handleClose} size="lg" isCloseOnOverlayClick={!uploading}>
            <ModalOverlay />
            <ModalContent>
                <ModalHeader>Upload File</ModalHeader>
                <ModalCloseButton isDisabled={uploading} />
                <ModalBody>
                    <VStack spacing={4}>
                        {!selectedFile ? (
                            <Box
                                {...getRootProps()}
                                w="100%"
                                p={8}
                                border="2px dashed"
                                borderColor={isDragActive ? "blue.500" : "gray.300"}
                                borderRadius="lg"
                                textAlign="center"
                                cursor="pointer"
                                bg={isDragActive ? "blue.50" : "gray.50"}
                                transition="all 0.2s"
                                _hover={{ borderColor: "blue.400", bg: "blue.50" }}
                            >
                                <input {...getInputProps()} />
                                <Icon as={FiUpload} w={12} h={12} color="gray.400" mb={4} />
                                {isDragActive ? (
                                    <Text color="blue.500" fontWeight="semibold">
                                        Drop the file here...
                                    </Text>
                                ) : (
                                    <>
                                        <Text fontWeight="semibold" mb={2}>
                                            Drag and drop a file here, or click to select
                                        </Text>
                                        <Text fontSize="sm" color="gray.500">
                                            Maximum file size: {formatFileSize(MAX_FILE_SIZE)}
                                        </Text>
                                    </>
                                )}
                            </Box>
                        ) : (
                            <Box w="100%">
                                <Alert status="info" borderRadius="md" mb={4}>
                                    <AlertIcon />
                                    File selected: {selectedFile.name}
                                </Alert>
                                
                                <Flex
                                    align="center"
                                    justify="space-between"
                                    p={4}
                                    borderWidth={1}
                                    borderRadius="md"
                                    bg="gray.50"
                                >
                                    <Flex align="center" flex={1} minW={0}>
                                        <Icon as={FiFile} w={6} h={6} color="blue.500" mr={3} />
                                        <Box flex={1} minW={0}>
                                            <Text fontWeight="semibold" noOfLines={1}>
                                                {selectedFile.name}
                                            </Text>
                                            <Text fontSize="sm" color="gray.500">
                                                {formatFileSize(selectedFile.size)}
                                            </Text>
                                        </Box>
                                    </Flex>
                                    {!uploading && (
                                        <IconButton
                                            icon={<FiX />}
                                            size="sm"
                                            colorScheme="red"
                                            variant="ghost"
                                            onClick={removeFile}
                                            aria-label="Remove file"
                                        />
                                    )}
                                </Flex>

                                {uploading && (
                                    <Box mt={4}>
                                        <Text fontSize="sm" mb={2}>Uploading... {uploadProgress}%</Text>
                                        <Progress value={uploadProgress} colorScheme="blue" borderRadius="full" />
                                    </Box>
                                )}
                            </Box>
                        )}
                    </VStack>
                </ModalBody>

                <ModalFooter>
                    <Button
                        variant="ghost"
                        mr={3}
                        onClick={handleClose}
                        isDisabled={uploading}
                    >
                        Cancel
                    </Button>
                    <Button
                        colorScheme="blue"
                        onClick={handleUpload}
                        isLoading={uploading}
                        loadingText="Uploading..."
                        isDisabled={!selectedFile || uploading}
                        leftIcon={<FiUpload />}
                    >
                        Upload
                    </Button>
                </ModalFooter>
            </ModalContent>
        </Modal>
    );
}
