import {
  Box,
  Button,
  Card,
  CardBody,
  Flex,
  FormControl,
  FormErrorMessage,
  FormLabel,
  Heading,
  Input,
  Link,
  Stack,
  Text,
  useColorModeValue,
} from '@chakra-ui/react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link as RouterLink } from 'react-router-dom';
import { BrandLogo } from '../../components/BrandLogo';
import { ColorModeToggle } from '../../components/ColorModeToggle';
import { useAuth } from '../../hooks/useAuth';

const schema = z.object({
  displayName: z.string().min(1, 'Display name is required').max(128),
  username: z.string().min(3, 'At least 3 characters').max(64),
  password: z.string().min(6, 'At least 6 characters').max(100),
});

type FormValues = z.infer<typeof schema>;

export function RegisterPage() {
  const { register: registerUser } = useAuth();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const bgGradient = useColorModeValue(
    'linear(to-br, brand.50, purple.50)',
    'linear(to-br, gray.900, brand.900)',
  );

  return (
    <Flex minH="100vh" align="center" justify="center" bgGradient={bgGradient} p={4} position="relative">
      <Box position="absolute" top={6} left={6}>
        <BrandLogo />
      </Box>
      <Box position="absolute" top={6} right={6}>
        <ColorModeToggle />
      </Box>
      <Stack spacing={6} w="full" maxW="md">
        <Card shadow="xl" borderRadius="2xl">
          <CardBody p={8}>
            <Stack spacing={5}>
              <Box textAlign="center">
                <Heading size="md">Create your account</Heading>
                <Text color="text-muted" fontSize="sm" mt={1}>
                  Start chatting in real time
                </Text>
              </Box>

              {registerUser.isError && (
                <Text color="red.400" fontSize="sm" textAlign="center">
                  Could not register — the username may already be taken
                </Text>
              )}

              <form onSubmit={handleSubmit((values) => registerUser.mutate(values))}>
                <Stack spacing={4}>
                  <FormControl isInvalid={Boolean(errors.displayName)}>
                    <FormLabel>Display name</FormLabel>
                    <Input placeholder="Alice Johnson" {...register('displayName')} autoFocus />
                    <FormErrorMessage>{errors.displayName?.message}</FormErrorMessage>
                  </FormControl>

                  <FormControl isInvalid={Boolean(errors.username)}>
                    <FormLabel>Username</FormLabel>
                    <Input placeholder="alice" {...register('username')} />
                    <FormErrorMessage>{errors.username?.message}</FormErrorMessage>
                  </FormControl>

                  <FormControl isInvalid={Boolean(errors.password)}>
                    <FormLabel>Password</FormLabel>
                    <Input type="password" placeholder="••••••••" {...register('password')} />
                    <FormErrorMessage>{errors.password?.message}</FormErrorMessage>
                  </FormControl>

                  <Button type="submit" size="lg" isLoading={registerUser.isPending} mt={2}>
                    Create account
                  </Button>
                </Stack>
              </form>

              <Text fontSize="sm" textAlign="center" color="text-muted">
                Already have an account?{' '}
                <Link as={RouterLink} to="/login" color="brand.500" fontWeight={600}>
                  Sign in
                </Link>
              </Text>
            </Stack>
          </CardBody>
        </Card>
      </Stack>
    </Flex>
  );
}
