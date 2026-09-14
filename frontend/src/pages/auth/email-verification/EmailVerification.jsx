import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
    Box,
    Button,
    FormControl,
    FormLabel,
    Heading,
    Input,
    Stack,
    Text,
    Alert,
    AlertIcon,
    AlertTitle,
    AlertDescription,
    Container,
    Spinner,
    Flex
} from '@chakra-ui/react';
import { verifyEmail, verifyEmailPost, resendVerificationEmail } from '../../../api/client.js';
import { errorNotification, successNotification } from '../../../utils/notification.js';

const EmailVerification = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [token, setToken] = useState(searchParams.get('token') || '');
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [verified, setVerified] = useState(false);
    const [resending, setResending] = useState(false);

    useEffect(() => {
        // If token exists in URL, verify immediately
        const tokenParam = searchParams.get('token');
        if (tokenParam) {
            setToken(tokenParam);
            handleVerify(tokenParam);
        }
    }, [searchParams]);

    const handleVerify = async (verifyToken) => {
        if (!verifyToken) {
            errorNotification("Error", "Verification token is required");
            return;
        }

        setLoading(true);
        try {
            // Try GET first, then POST if that fails
            try {
                await verifyEmail(verifyToken);
            } catch (e) {
                if (e.response?.status === 405) {
                    // Method not allowed, try POST
                    await verifyEmailPost(verifyToken);
                } else {
                    throw e;
                }
            }
            successNotification("Success", "Email verified successfully! You can now login.");
            setVerified(true);
            setTimeout(() => {
                navigate("/login");
            }, 2000);
        } catch (err) {
            const errorMessage = err.response?.data?.message || 
                               err.response?.data?.error || 
                               err.message || 
                               "Verification failed. The token may be invalid or expired.";
            errorNotification("Verification Failed", errorMessage);
        } finally {
            setLoading(false);
        }
    };

    const handleResend = async () => {
        if (!email) {
            errorNotification("Error", "Please enter your email address");
            return;
        }

        setResending(true);
        try {
            await resendVerificationEmail(email);
            successNotification("Success", "Verification email sent! Please check your inbox.");
        } catch (err) {
            const errorMessage = err.response?.data?.message || 
                               err.response?.data?.error || 
                               err.message || 
                               "Failed to resend verification email.";
            errorNotification("Error", errorMessage);
        } finally {
            setResending(false);
        }
    };

    if (verified) {
        return (
            <Container maxW="md" centerContent mt={20}>
                <Alert status="success" borderRadius="md">
                    <AlertIcon />
                    <Box>
                        <AlertTitle>Email Verified!</AlertTitle>
                        <AlertDescription>
                            Your email has been successfully verified. Redirecting to login...
                        </AlertDescription>
                    </Box>
                </Alert>
            </Container>
        );
    }

    return (
        <Container maxW="md" centerContent mt={20}>
            <Box
                p={8}
                borderWidth={1}
                borderRadius="lg"
                boxShadow="lg"
                w="full"
            >
                <Heading size="lg" mb={4}>Verify Your Email</Heading>
                
                {token ? (
                    <Stack spacing={4}>
                        <Text>Click the button below to verify your email address:</Text>
                        <Button
                            colorScheme="blue"
                            onClick={() => handleVerify(token)}
                            isLoading={loading}
                            loadingText="Verifying..."
                        >
                            Verify Email
                        </Button>
                    </Stack>
                ) : (
                    <Stack spacing={4}>
                        <Text mb={4}>
                            Enter your email address and we'll send you a new verification link.
                        </Text>
                        <FormControl>
                            <FormLabel>Email Address</FormLabel>
                            <Input
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="you@example.com"
                            />
                        </FormControl>
                        <Button
                            colorScheme="blue"
                            onClick={handleResend}
                            isLoading={resending}
                            loadingText="Sending..."
                            w="full"
                        >
                            Resend Verification Email
                        </Button>
                    </Stack>
                )}

                <Flex mt={6} justify="center">
                    <Button variant="link" onClick={() => navigate("/login")}>
                        Back to Login
                    </Button>
                </Flex>
            </Box>
        </Container>
    );
};

export default EmailVerification;
