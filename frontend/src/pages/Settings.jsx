import {
    Box,
    Button,
    Container,
    Heading,
    Text,
    VStack,
    Divider,
    useColorModeValue
} from '@chakra-ui/react';
import Sidebar from '../components/layout/Sidebar.jsx';
import { useAuth } from '../AuthProvider.jsx';
import { useNavigate } from 'react-router-dom';

const Settings = () => {
    const { user, logOut } = useAuth();
    const navigate = useNavigate();
    const muted = useColorModeValue('gray.600', 'gray.400');

    return (
        <Sidebar>
            <Container maxW="container.md" py={8}>
                <Heading size="lg" mb={6}>Settings</Heading>
                <VStack align="stretch" spacing={6}>
                    <Box>
                        <Text fontWeight="semibold" mb={1}>Account</Text>
                        <Text color={muted}>{user?.email || 'Unknown user'}</Text>
                    </Box>
                    <Divider />
                    <Box>
                        <Text fontWeight="semibold" mb={2}>Profile</Text>
                        <Text color={muted} mb={4}>
                            Profile and password management coming soon.
                        </Text>
                        <Button
                            colorScheme="red"
                            variant="outline"
                            onClick={() => {
                                logOut();
                                navigate('/login');
                            }}
                        >
                            Sign out
                        </Button>
                    </Box>
                </VStack>
            </Container>
        </Sidebar>
    );
};

export default Settings;
