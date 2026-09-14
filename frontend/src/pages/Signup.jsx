import { useAuth } from '../AuthProvider.jsx';
import { useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { Flex, Heading, Image, Link, Stack, Text, Box, Button, FormLabel, Input, Alert, AlertIcon } from '@chakra-ui/react';
import { Formik, Form, useField } from 'formik';
import * as Yup from 'yup';
import { register } from '../api/client.js';
import { errorNotification, successNotification } from '../notification.js';

const MyTextInput = ({label, ...props}) => {
    const [field, meta] = useField(props);
    return (
        <Box>
            <FormLabel htmlFor={props.id || props.name}>{label}</FormLabel>
            <Input className="text-input" {...field} {...props} />
            {meta.touched && meta.error ? (
                <Alert className="error" status={"error"} mt={2}>
                    <AlertIcon/>
                    {meta.error}
                </Alert>
            ) : null}
        </Box>
    );
};

const RegistrationForm = ({onSuccess}) => {
    const navigate = useNavigate();

    return (
        <Formik
            validateOnMount={true}
            validationSchema={
                Yup.object({
                    name: Yup.string()
                        .min(2, "Name must be at least 2 characters")
                        .max(100, "Name must be less than 100 characters")
                        .required("Name is required"),
                    email: Yup.string()
                        .email("Must be valid email")
                        .required("Email is required"),
                    password: Yup.string()
                        .min(8, "Password must be at least 8 characters")
                        .max(100, "Password must be less than 100 characters")
                        .required("Password is required"),
                    confirmPassword: Yup.string()
                        .oneOf([Yup.ref('password'), null], "Passwords must match")
                        .required("Please confirm your password")
                })
            }
            initialValues={{name: '', email: '', password: '', confirmPassword: ''}}
            onSubmit={(values, {setSubmitting}) => {
                setSubmitting(true);
                register({
                    name: values.name,
                    email: values.email,
                    password: values.password
                }).then(() => {
                    successNotification(
                        "Registration Successful",
                        "Please check your email to verify your account before logging in."
                    );
                    setTimeout(() => {
                        navigate("/login");
                    }, 2000);
                }).catch(err => {
                    const errorMessage = err.response?.data?.message || 
                                       err.response?.data?.error || 
                                       err.message || 
                                       "Registration failed. Please try again.";
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
                        <MyTextInput
                            label={"Full Name"}
                            name={"name"}
                            type={"text"}
                            placeholder={"John Doe"}
                        />
                        <MyTextInput
                            label={"Email"}
                            name={"email"}
                            type={"email"}
                            placeholder={"you@example.com"}
                        />
                        <MyTextInput
                            label={"Password"}
                            name={"password"}
                            type={"password"}
                            placeholder={"Minimum 8 characters"}
                        />
                        <MyTextInput
                            label={"Confirm Password"}
                            name={"confirmPassword"}
                            type={"password"}
                            placeholder={"Re-enter your password"}
                        />

                        <Button
                            type={"submit"}
                            disabled={!isValid || isSubmitting}
                            colorScheme="blue"
                            isLoading={isSubmitting}>
                            Sign Up
                        </Button>
                    </Stack>
                </Form>
            )}
        </Formik>
    );
};

const Signup = () => {
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
                    <Image
                        src={"https://user-images.githubusercontent.com/40702606/210880158-e7d698c2-b19a-4057-b415-09f48a746753.png"}
                        boxSize={"200px"}
                        alt={"Logo"}
                        alignSelf={"center"}
                    />
                    <Heading fontSize={'2xl'} mb={15}>Create your account</Heading>
                    <RegistrationForm/>
                    <Link color={"blue.500"} href={"/"}>
                        Already have an account? Login now.
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
                    Secure file storage and management made easy
                </Text>
            </Flex>
        </Stack>
    );
};

export default Signup;
