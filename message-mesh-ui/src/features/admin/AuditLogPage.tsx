import { useEffect, useState } from 'react';
import {
  Badge,
  Box,
  Button,
  Center,
  Flex,
  Heading,
  HStack,
  Input,
  InputGroup,
  InputLeftElement,
  Spacer,
  Spinner,
  Table,
  TableContainer,
  Tbody,
  Td,
  Text,
  Th,
  Thead,
  Tr,
} from '@chakra-ui/react';
import {
  FiActivity,
  FiChevronLeft,
  FiChevronRight,
  FiDownload,
  FiSearch,
} from 'react-icons/fi';
import { AdminHeader } from './AdminHeader';
import { EmptyState } from '../../components/EmptyState';
import { useAuditLog } from '../../hooks/useAuditLog';
import { formatDayLabel, formatTime } from '../../utils/formatters';
import { downloadCsv } from '../../utils/csv';

const PAGE_SIZE = 20;

// Convert a yyyy-mm-dd input into an ISO instant; `to` is pushed to end-of-day.
function toInstant(dateStr: string, endOfDay = false): string | undefined {
  if (!dateStr) return undefined;
  const d = new Date(dateStr);
  if (Number.isNaN(d.getTime())) return undefined;
  if (endOfDay) d.setHours(23, 59, 59, 999);
  return d.toISOString();
}

export function AuditLogPage() {
  const [actor, setActor] = useState('');
  const [action, setAction] = useState('');
  const [debouncedActor, setDebouncedActor] = useState('');
  const [debouncedAction, setDebouncedAction] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [page, setPage] = useState(0);

  useEffect(() => {
    const handle = setTimeout(() => {
      setDebouncedActor(actor.trim());
      setDebouncedAction(action.trim());
      setPage(0);
    }, 300);
    return () => clearTimeout(handle);
  }, [actor, action]);

  useEffect(() => {
    setPage(0);
  }, [fromDate, toDate]);

  const { data, isLoading, isError, isFetching } = useAuditLog({
    actor: debouncedActor,
    action: debouncedAction,
    from: toInstant(fromDate),
    to: toInstant(toDate, true),
    page,
    size: PAGE_SIZE,
  });

  const rows = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;
  const canPrev = page > 0;
  const canNext = page + 1 < totalPages;

  const exportCsv = () => {
    downloadCsv(
      'audit-log.csv',
      ['When', 'Actor', 'Action', 'Target', 'Details'],
      rows.map((e) => [
        new Date(e.createdAt).toLocaleString(),
        e.actorUsername,
        e.action,
        e.targetType ? `${e.targetType}${e.targetId ? `: ${e.targetId}` : ''}` : '',
        e.details ?? '',
      ]),
    );
  };

  return (
    <Flex direction="column" h="100vh" overflow="hidden">
      <AdminHeader />

      <Box flex={1} overflowY="auto" px={{ base: 4, md: 8 }} py={6}>
        <Box maxW="1100px" mx="auto">
          <Flex
            align={{ base: 'start', md: 'center' }}
            mb={6}
            gap={3}
            direction={{ base: 'column', md: 'row' }}
          >
            <Box>
              <Heading size="lg">Audit log</Heading>
              <Text color="text-muted" fontSize="sm">
                {data
                  ? `${totalElements} recorded ${totalElements === 1 ? 'event' : 'events'}`
                  : 'Track administrative activity'}
              </Text>
            </Box>
            <Spacer />
            <HStack flexWrap="wrap" gap={2}>
              <InputGroup maxW="180px">
                <InputLeftElement pointerEvents="none">
                  <FiSearch />
                </InputLeftElement>
                <Input
                  placeholder="Actor"
                  value={actor}
                  onChange={(e) => setActor(e.target.value)}
                />
              </InputGroup>
              <InputGroup maxW="180px">
                <InputLeftElement pointerEvents="none">
                  <FiSearch />
                </InputLeftElement>
                <Input
                  placeholder="Action"
                  value={action}
                  onChange={(e) => setAction(e.target.value)}
                />
              </InputGroup>
              <Input
                type="date"
                maxW="160px"
                aria-label="From date"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
              />
              <Input
                type="date"
                maxW="160px"
                aria-label="To date"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
              />
              <Button
                leftIcon={<FiDownload />}
                size="sm"
                variant="outline"
                onClick={exportCsv}
                isDisabled={rows.length === 0}
              >
                Export CSV
              </Button>
            </HStack>
          </Flex>

          {isLoading ? (
            <Center py={20}>
              <Spinner size="lg" color="brand.500" />
            </Center>
          ) : isError ? (
            <EmptyState
              icon={FiActivity}
              title="Couldn't load audit log"
              description="Something went wrong. Try again shortly."
            />
          ) : rows.length === 0 ? (
            <EmptyState
              icon={FiActivity}
              title="No events found"
              description={
                debouncedActor || debouncedAction
                  ? 'No events match your filters.'
                  : 'No activity has been recorded yet.'
              }
            />
          ) : (
            <>
              <TableContainer
                borderWidth="1px"
                borderColor="panel-border"
                borderRadius="xl"
                overflow="hidden"
              >
                <Table size="sm" variant="simple">
                  <Thead bg="app-bg">
                    <Tr>
                      <Th>When</Th>
                      <Th>Actor</Th>
                      <Th>Action</Th>
                      <Th>Target</Th>
                      <Th>Details</Th>
                    </Tr>
                  </Thead>
                  <Tbody>
                    {rows.map((e) => (
                      <Tr key={e.id}>
                        <Td whiteSpace="nowrap">
                          {formatDayLabel(e.createdAt)} · {formatTime(e.createdAt)}
                        </Td>
                        <Td fontWeight={600}>{e.actorUsername}</Td>
                        <Td>
                          <Badge colorScheme="brand" variant="subtle">
                            {e.action}
                          </Badge>
                        </Td>
                        <Td>
                          {e.targetType ? (
                            <Text fontSize="xs" color="text-muted" noOfLines={1}>
                              {e.targetType}
                              {e.targetId ? `: ${e.targetId}` : ''}
                            </Text>
                          ) : (
                            '—'
                          )}
                        </Td>
                        <Td maxW="320px">
                          <Text fontSize="xs" color="text-muted" noOfLines={2}>
                            {e.details || '—'}
                          </Text>
                        </Td>
                      </Tr>
                    ))}
                  </Tbody>
                </Table>
              </TableContainer>

              <Flex mt={4} align="center" gap={3}>
                <Text color="text-muted" fontSize="sm">
                  Page {page + 1} of {Math.max(totalPages, 1)}
                </Text>
                {isFetching && <Spinner size="sm" color="brand.500" />}
                <Spacer />
                <HStack>
                  <Button
                    leftIcon={<FiChevronLeft />}
                    size="sm"
                    variant="outline"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    isDisabled={!canPrev}
                  >
                    Prev
                  </Button>
                  <Button
                    rightIcon={<FiChevronRight />}
                    size="sm"
                    variant="outline"
                    onClick={() => setPage((p) => p + 1)}
                    isDisabled={!canNext}
                  >
                    Next
                  </Button>
                </HStack>
              </Flex>
            </>
          )}
        </Box>
      </Box>
    </Flex>
  );
}
