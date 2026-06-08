export type SocialPreference = 'TALK_TO_ME' | 'DONT_TALK_TO_ME' | 'NONE'

export interface Desk {
  id: string
  name: string
  neighborhood: string | null
  x: number
  y: number
  rotation: number
}

export interface Employee {
  id: string
  name: string
  role: string
  location: string
}

export interface OrgNode {
  employeeId: string
  parentId: string | null
  childrenIds: string[]
  depth: number
  orgPath: string[]
}

export interface BookingRequest {
  employeeId: string
  socialPreference: SocialPreference
  windowSeat: boolean
  feelingLucky: boolean
}

export interface BookingCollection {
  bookings: BookingRequest[]
  totalCapacity: number
  remaining: number
}

export interface AssignmentCollection {
  deskByEmployeeId: Record<string, string>
  employeeByDeskId: Record<string, string>
}

export interface AssignmentScore {
  totalQapCost: number
  teamCohesion: number
  managerProximity: number
  socialSatisfaction: number
  windowHitRate: number
}