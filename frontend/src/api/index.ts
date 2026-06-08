import { RestApplicationClient } from '../generated/api'
import { FetchHttpClient } from './client'

const client = new RestApplicationClient(new FetchHttpClient())

export const getDesks = () => client.getDesks()
export const getEmployees = () => client.getEmployees()
export const getOrgNodes = () => client.getOrgNodes()
export const getBookings = () => client.getBookings()
export const addBooking = (request: Parameters<typeof client.addBooking>[0]) => client.addBooking(request)
export const getAssignments = () => client.getAssignment()
export const runAssignment = () => client.run()
export const getScore = () => client.getScore()
