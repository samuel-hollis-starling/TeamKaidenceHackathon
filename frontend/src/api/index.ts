import { FetchHttpClient } from './client'
import { RestApplicationClient } from '../generated/api'
import type {
  Desk,
  Employee,
  OrgNode,
  BookingCollection,
  BookingRequest,
  AssignmentCollection,
  AssignmentScore,
} from '../types'

const client = new RestApplicationClient(new FetchHttpClient())

export const getDesks = (): Promise<Desk[]> => client.getDesks()
export const getEmployees = (): Promise<Employee[]> => client.getEmployees()
export const getOrgNodes = (): Promise<OrgNode[]> => client.getOrgNodes()
export const getBookings = (): Promise<BookingCollection> => client.getBookings()
export const addBooking = (req: BookingRequest): Promise<BookingRequest> => client.addBooking(req)
export const getAssignments = (): Promise<AssignmentCollection> => client.getAssignment()
export const runAssignment = (): Promise<AssignmentCollection> => client.run()
export const getScore = (): Promise<AssignmentScore> => client.getScore()
