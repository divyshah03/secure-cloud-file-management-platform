import React from 'react';
import {
    Avatar,
    Box,
    CloseButton,
    Drawer,
    DrawerContent,
    Flex,
    HStack,
    Icon,
    IconButton,
    Menu,
    MenuButton,
    MenuDivider,
    MenuItem,
    MenuList,
    Text,
    useColorModeValue,
    useDisclosure,
    VStack,
    Image
} from '@chakra-ui/react';

import {
    FiBell,
    FiChevronDown,
    FiHome,
    FiMenu,
    FiSettings,
    FiFile
} from 'react-icons/fi';
import { useAuth } from '../../AuthProvider.jsx';
import { useNavigate, useLocation } from 'react-router-dom';

const LinkItems = [
    {name: 'Dashboard', route: '/dashboard', icon: FiHome},
    {name: 'My Files', route: '/dashboard/files', icon: FiFile},
    {name: 'Settings', route: '/dashboard/settings', icon: FiSettings},
];

export default function Sidebar({children}) {
    const {isOpen, onOpen, onClose} = useDisclosure();
    return (
        <Box minH="100vh" bg={useColorModeValue('gray.100', 'gray.900')}>
            <SidebarContent
                onClose={onClose}
                display={{base: 'none', md: 'block'}}
            />
            <Drawer
                autoFocus={false}
                isOpen={isOpen}
                placement="left"
                onClose={onClose}
                returnFocusOnClose={false}
                onOverlayClick={onClose}
                size="full">
                <DrawerContent>
                    <SidebarContent onClose={onClose}/>
                </DrawerContent>
            </Drawer>
            <MobileNav onOpen={onOpen}/>
            <Box ml={{base: 0, md: 60}} p="4">
                {children}
            </Box>
        </Box>
    );
}

const SidebarContent = ({onClose, ...rest}) => {
    return (
        <Box
            transition="3s ease"
            bg={useColorModeValue('white', 'gray.900')}
            borderRight="1px"
            borderRightColor={useColorModeValue('gray.200', 'gray.700')}
            w={{base: 'full', md: 60}}
            pos="fixed"
            h="full"
            {...rest}>
            <Flex h="20" flexDirection="column" alignItems="center" mx="8" mb={75} mt={2} justifyContent="space-between">
                <Text fontSize="2xl" fontFamily="monospace" fontWeight="bold" mb={5}>
                    Cloud File Manager
                </Text>
                <Image
                    borderRadius='full'
                    boxSize='75px'
                    src='https://user-images.githubusercontent.com/40702606/210880158-e7d698c2-b19a-4057-b415-09f48a746753.png'
                    alt='Logo'
                />
                <CloseButton display={{base: 'flex', md: 'none'}} onClick={onClose}/>
            </Flex>
            {LinkItems.map((link) => (
                <NavItem key={link.name} route={link.route} icon={link.icon}>
                    {link.name}
                </NavItem>
            ))}
        </Box>
    );
};

const NavItem = ({icon, route, children, ...rest}) => {
    const navigate = useNavigate();
    const location = useLocation();
    const isActive = location.pathname === route;
    
    return (
        <Box
            onClick={() => navigate(route)}
            style={{textDecoration: 'none'}}
            _focus={{boxShadow: 'none'}}
            cursor="pointer"
        >
            <Flex
                align="center"
                p="4"
                mx="4"
                borderRadius="lg"
                role="group"
                bg={isActive ? 'blue.500' : 'transparent'}
                color={isActive ? 'white' : 'inherit'}
                _hover={{
                    bg: 'blue.400',
                    color: 'white',
                }}
                {...rest}>
                {icon && (
                    <Icon
                        mr="4"
                        fontSize="16"
                        _groupHover={{
                            color: 'white',
                        }}
                        as={icon}
                    />
                )}
                {children}
            </Flex>
        </Box>
    );
};

const MobileNav = ({onOpen, ...rest}) => {
    const { logOut, user } = useAuth();
    const navigate = useNavigate();
    
    return (
        <Flex
            ml={{base: 0, md: 60}}
            px={{base: 4, md: 4}}
            height="20"
            alignItems="center"
            bg={useColorModeValue('white', 'gray.900')}
            borderBottomWidth="1px"
            borderBottomColor={useColorModeValue('gray.200', 'gray.700')}
            justifyContent={{base: 'space-between', md: 'flex-end'}}
            {...rest}>
            <IconButton
                display={{base: 'flex', md: 'none'}}
                onClick={onOpen}
                variant="outline"
                aria-label="open menu"
                icon={<FiMenu/>}
            />

            <Text
                display={{base: 'flex', md: 'none'}}
                fontSize="2xl"
                fontFamily="monospace"
                fontWeight="bold">
                Cloud File Manager
            </Text>

            <HStack spacing={{base: '0', md: '6'}}>
                <IconButton
                    size="lg"
                    variant="ghost"
                    aria-label="open menu"
                    icon={<FiBell/>}
                />
                <Flex alignItems={'center'}>
                    <Menu>
                        <MenuButton
                            py={2}
                            transition="all 0.3s"
                            _focus={{boxShadow: 'none'}}>
                            <HStack>
                                <Avatar
                                    size={'sm'}
                                    name={user?.email || 'User'}
                                    bg="blue.500"
                                />
                                <VStack
                                    display={{base: 'none', md: 'flex'}}
                                    alignItems="flex-start"
                                    spacing="1px"
                                    ml="2">
                                    <Text fontSize="sm">{user?.email || 'User'}</Text>
                                    {user?.roles && user.roles.length > 0 && (
                                        <Text fontSize="xs" color="gray.600">
                                            {user.roles[0]}
                                        </Text>
                                    )}
                                </VStack>
                                <Box display={{base: 'none', md: 'flex'}}>
                                    <FiChevronDown/>
                                </Box>
                            </HStack>
                        </MenuButton>
                        <MenuList
                            bg={useColorModeValue('white', 'gray.900')}
                            borderColor={useColorModeValue('gray.200', 'gray.700')}>
                            <MenuItem onClick={() => navigate('/dashboard/files')}>My Files</MenuItem>
                            <MenuItem onClick={() => navigate('/dashboard/settings')}>Settings</MenuItem>
                            <MenuDivider/>
                            <MenuItem onClick={() => {
                                logOut();
                                navigate('/login');
                            }}>
                                Sign out
                            </MenuItem>
                        </MenuList>
                    </Menu>
                </Flex>
            </HStack>
        </Flex>
    );
};
