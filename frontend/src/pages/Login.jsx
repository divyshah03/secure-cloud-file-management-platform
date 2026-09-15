import {
    Button,
    Flex,
    Heading,
    Icon,
    Link,
    Stack,
    Text,
} from '@chakra-ui/react';
import { FiCloud } from 'react-icons/fi';
import {Formik, Form} from "formik";
import * as Yup from 'yup';
import { useAuth } from '../AuthProvider.jsx';
import { errorNotification, successNotification } from '../notification.js';
import {useNavigate, Link as RouterLink} from "react-router-dom";
import {useEffect} from "react";
import FormTextInput from '../components/forms/FormTextInput.jsx';

const LoginForm = () => {
    const { login } = useAuth();
    const navigate = useNavigate();

    return (
        <Formik
            validateOnMount={true}
            validationSchema={
                Yup.object({
                    email: Yup.string()
                        .email("Must be valid email")
                        .required("Email is required"),
                    password: Yup.string()
                        .min(8, "Password must be at least 8 characters")
                        .required("Password is required")
                })
            }
            initialValues={{email: '', password: ''}}
            onSubmit={(values, {setSubmitting}) => {
                setSubmitting(true);
                login({
                    email: values.email,
                    password: values.password
                }).then(res => {
                    successNotification("Success", "Logged in successfully");
                    navigate("/dashboard");
                }).catch(err => {
                    const errorMessage = err.response?.data?.message || 
                                       err.response?.data?.error || 
                                       err.message || 
                                       "Login failed. Please check your credentials.";
                    errorNotification(
                        err.response?.status || "Error",
                        errorMessage
                    );
                }).finally(() => {
                    setSubmitting(false);
                });
            }}>

            {({isValid, isSubmitting}) => (
                <Form>
                    <Stack mt={15} spacing={15}>
                        <FormTextInput
                            label={"Email"}
                            name={"email"}
                            type={"email"}
                            placeholder={"you@example.com"}
                        />
                        <FormTextInput
                            label={"Password"}
                            name={"password"}
                            type={"password"}
                            placeholder={"Enter your password"}
                        />

                        <Button
                            type={"submit"}
                            disabled={!isValid || isSubmitting}
                            colorScheme="blue"
                            isLoading={isSubmitting}>
                            Login
                        </Button>
                    </Stack>
                </Form>
            )}
        </Formik>
    );
};

const Login = () => {
    const { user, isAuthenticated } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        if (isAuthenticated() && user) {
            navigate("/dashboard");
        }
    }, [user, isAuthenticated, navigate]);

    return (
        <Stack minH={'100vh'} direction={{base: 'column', md: 'row'}}>
            <Flex p={8} flex={1} alignItems={'center'} justifyContent={'center'}>
                <Stack spacing={4} w={'full'} maxW={'md'}>
                    <Flex
                        boxSize={"120px"}
                        borderRadius={"full"}
                        alignSelf={"center"}
                        alignItems={"center"}
                        justifyContent={"center"}
                        bgGradient={"linear(to-br, blue.500, purple.600)"}
                    >
                        <Icon as={FiCloud} boxSize={"60px"} color={"white"} />
                    </Flex>
                    <Heading fontSize={'2xl'} mb={15}>Sign in to Cloud File Manager</Heading>
                    <LoginForm/>
                    <Link as={RouterLink} color={"blue.500"} to={"/signup"}>
                        Don't have an account? Sign up now.
                    </Link>
                </Stack>
            </Flex>
            <Flex
                flex={1}
                p={10}
                flexDirection={"column"}
                alignItems={"center"}
                justifyContent={"center"}
                bgGradient={{sm: 'linear(to-r, blue.600, purple.600)'}}
            >
                <Text fontSize={"6xl"} color={'white'} fontWeight={"bold"} mb={5}>
                    Cloud File Manager
                </Text>
                <Text fontSize={"xl"} color={'white'} textAlign="center">
                    Securely upload, manage, and organize your files
                </Text>
            </Flex>
        </Stack>
    );
};

export default Login;
