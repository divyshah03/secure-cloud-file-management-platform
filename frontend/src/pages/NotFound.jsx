import { Box, Button, Heading, Text, VStack } from '@chakra-ui/react';
import { Link as RouterLink } from 'react-router-dom';

const NotFound = () => (
    <Box minH="100vh" display="flex" alignItems="center" justifyContent="center" p={8}>
        <VStack spacing={4}>
            <Heading size="2xl">404</Heading>
            <Text fontSize="lg">Page not found</Text>
            <Button as={RouterLink} to="/dashboard" colorScheme="blue">
                Go to Dashboard
            </Button>
        </VStack>
    </Box>
);

export default NotFound;
