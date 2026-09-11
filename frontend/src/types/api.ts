// Shared API contract types mirroring the Quarkus backend DTOs.

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  timestamp: string;
  correlationId: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  correlationId: string;
  details?: string[];
}

export interface Page<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

// ---------- auth ----------
export interface AuthData {
  accessToken: string;
  refreshToken: string;
  userId: number;
  email: string;
  fullName: string;
  role: 'CUSTOMER' | 'SELLER' | 'TECHNICIAN' | 'ADMIN';
}

// ---------- listings ----------
export interface ListingDto {
  id: string;
  unitId: string;
  sellerId: number;
  title: string;
  description: string | null;
  askingPrice: number;
  status: 'DRAFT' | 'PENDING_REVIEW' | 'ACTIVE' | 'PAUSED' | 'SOLD' | 'REMOVED';
  gradeSnapshot: string;
  images: string | null; // JSON array of URLs, e.g. "[\"https://...\"]"
}

export interface ListingSearchParams {
  minPrice?: number;
  maxPrice?: number;
  grade?: string;
  sort?: 'newest' | 'priceAsc' | 'priceDesc';
  page?: number;
  size?: number;
}

export interface CreateListingRequest {
  unitId: string;
  title: string;
  description?: string;
  askingPrice: number;
  images?: string;
}

export interface UpdateListingRequest {
  title?: string;
  description?: string;
  askingPrice?: number;
  images?: string;
}

// ---------- orders ----------
export interface MasterOrderDto {
  id: string;
  orderNumber: string;
  buyerId: number;
  totalAmount: number;
  paymentStatus: string;
  createdAt: string;
}

export interface FulfillmentOrderDto {
  id: string;
  masterOrderId: string;
  sellerId: number;
  unitId: string;
  subtotalAmount: number;
  platformFeeAmount: number;
  sellerNetAmount: number;
  fulfillmentStatus: 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'COMPLETED' | 'DISPUTED' | 'CANCELLED';
  escrowStatus: string;
  trackingNumber: string | null;
  courierName: string | null;
  shippedAt: string | null;
  deliveredAt: string | null;
  createdAt: string;
}

export interface ShipRequest {
  trackingNumber: string;
  courierName: string;
}

// ---------- sellers ----------
export interface SellerMetricsResponse {
  sellerId: number;
  storeName: string;
  storeSlug: string;
  reputationScore: number;
  returnRate: number;
  disputeRate: number;
  responseRate: number;
  completedOrders: number;
  activeDisputes: number;
  kycStatus: string;
}

// ---------- disputes ----------
export interface DisputeResponse {
  id: string;
  fulfillmentOrderId: string;
  buyerId: number;
  sellerId: number;
  reason: string;
  claimDescription: string;
  status: string;
  resolutionType: string | null;
  buyerRefundAmount: number;
  sellerPayoutAmount: number;
  createdAt: string;
}

export interface CreateDisputeRequest {
  fulfillmentOrderId: string;
  sellerId: number;
  reason: string;
  claimDescription: string;
}

export interface ResolveDisputeRequest {
  resolutionType: 'FULL_REFUND' | 'PARTIAL_REFUND' | 'RELEASE_PAYMENT' | 'REPAIR' | 'REPLACEMENT';
  buyerRefundAmount?: number;
  sellerPayoutAmount?: number;
  resolutionNotes?: string;
}

// ---------- inspections ----------
export interface InspectionResponse {
  id: string;
  unitId: string;
  technicianId: number;
  physicalScore: number;
  hardwareScore: number;
  softwareScore: number;
  finalCalculatedGrade: string;
  estimatedRepairCost: number;
  technicianNotes: string | null;
  createdAt: string;
}

export interface CreateInspectionRequest {
  unitId: string;
  physicalScore: number;
  hardwareScore: number;
  softwareScore: number;
  hasCriticalFailure: boolean;
  estimatedRepairCost?: number;
  technicianNotes?: string;
}

// ---------- checkout ----------
export interface ReservationResponse {
  reservationId: string;
  unitId: string;
  listingId: string;
  token: string;
  expiresAt: string;
  remainingSeconds: number;
}

export interface OrderConfirmationResponse {
  masterOrderId: string;
  orderNumber: string;
  fulfillmentOrderId: string;
  unitId: string;
  totalAmount: number;
  platformFeeAmount: number;
  sellerNetAmount: number;
  paymentStatus: string;
  escrowStatus: string;
  createdAt: string;
}

// ---------- trade-in ----------
export interface TradeInCalculationRequest {
  msrp: number;
  annualDepreciationRate: number;
  releaseDate: string;
  condition: 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR' | 'DAMAGED';
  functionality: 'FULLY_FUNCTIONAL' | 'MINOR_ISSUES' | 'MAJOR_ISSUES' | 'NOT_WORKING';
  batteryHealthPercentage: number;
  hasCompleteAccessories: boolean;
  estimatedRepairCost?: number;
}

export interface TradeInOfferResponse {
  estimatedOffer: number;
  baseDepreciatedValue: number;
  conditionMultiplier: number;
  batteryMultiplier: number;
  accessoriesMultiplier: number;
  platformMarginRate: number;
}

export interface TradeInRequestResponse {
  id: string;
  productModelId: string;
  declaredCondition: string;
  declaredFunctionality: string;
  declaredBatteryHealth: number;
  hasCompleteAccessories: boolean;
  estimatedOffer: number;
  status: string;
  createdAt: string;
}

export interface SubmitTradeInRequest {
  productModelId: string;
  msrp: number;
  annualDepreciationRate: number;
  releaseDate: string;
  declaredCondition: TradeInCalculationRequest['condition'];
  declaredFunctionality: TradeInCalculationRequest['functionality'];
  batteryHealthPercentage: number;
  hasCompleteAccessories: boolean;
  estimatedRepairCost?: number;
}

// ---------- warranties ----------
export interface WarrantyDto {
  id: string;
  unitId: string;
  ownerId: number;
  fulfillmentOrderId: string;
  startsAt: string;
  expiresAt: string;
  policyTier: string;
  isVoided: boolean;
}

// ---------- reviews ----------
export interface ReviewResponse {
  id: string;
  fulfillmentOrderId: string;
  unitId: string;
  sellerId: number;
  buyerId: number;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface SubmitReviewRequest {
  fulfillmentOrderId: string;
  rating: number;
  comment?: string;
}

// ---------- catalog models ----------
export interface ProductModelDto {
  id: string;
  categoryId: string;
  brand: string;
  modelName: string;
  slug: string;
  msrp: number;
  annualDepreciationRate: number;
  releaseDate: string;
}

// ---------- cart ----------
export interface CartItemDto {
  cartItemId: string;
  listingId: string;
  unitId: string;
  price: number;
  quantity: number;
  listingTitle: string;
  listingStatus: string;
}

export interface CartDto {
  items: CartItemDto[];
  totalItems: number;
  distinctListings: number;
}

// ---------- returns ----------
export interface ReturnResponse {
  id: string;
  fulfillmentOrderId: string;
  buyerId: number;
  reason: string;
  description: string;
  evidenceImages: string[];
  status: string;
  rejectionReason: string | null;
  courierName: string | null;
  trackingNumber: string | null;
  inspectionNotes: string | null;
  refundAmount: number;
  resolvedAt: string | null;
  createdAt: string;
}

export interface CreateReturnRequest {
  fulfillmentOrderId: string;
  reason: string;
  description: string;
  evidenceImages?: string[];
}

// ---------- notifications ----------
export interface NotificationResponse {
  id: string;
  recipientId: number;
  title: string;
  body: string;
  category: string;
  referenceType: string | null;
  referenceId: string | null;
  isRead: boolean;
  readAt: string | null;
  createdAt: string;
}

export interface NotificationsPageResponse {
  items: NotificationResponse[];
  total: number;
  page: number;
  size: number;
  unreadCount: number;
}

// ---------- repairs ----------
export interface RepairTicketDto {
  id: string;
  unitId: string;
  technicianId: number;
  issueDescription: string;
  replacedComponents: string | null; // JSON array of replaced components
  partsCost: number;
  status: 'OPEN' | 'DIAGNOSING' | 'IN_PROGRESS' | 'QC_PENDING' | 'COMPLETED' | 'CANCELLED';
  createdAt: string;
  updatedAt: string;
}

export interface CreateRepairTicketDto {
  unitId: string;
  issueDescription: string;
  initialPartsCost?: number;
}

export interface ComponentReplacementDto {
  componentName: string;
  serialNumber?: string;
  cost?: number;
}

export interface SubmitQcRequest {
  components: ComponentReplacementDto[];
  totalPartsCost?: number;
}

export interface CompleteRepairRequest {
  regraded: boolean;
  newPhysicalScore?: number;
  newHardwareScore?: number;
  newSoftwareScore?: number;
  technicianNotes?: string;
}

// ---------- escrow (admin) ----------
export interface EscrowStatsDto {
  escrowStatusCounts: Record<string, number>;
  totalHeldAmount: number;
}

// ---------- profile ----------
export interface ProfileDto {
  id: number;
  email: string;
  fullName: string;
  phoneNumber: string | null;
  role: string;
  verified: boolean;
  kycStatus: string;
  address: string;
}

export interface UpdateProfileRequest {
  fullName: string;
  phoneNumber?: string;
  address?: string;
}

export interface KycStatusDto {
  userId: number;
  kycStatus: string;
  kycDocumentType: string | null;
  kycDocumentReference: string | null;
  nationalId: string | null;
  dateOfBirth: string | null;
  kycSubmittedAt: string | null;
  kycVerifiedAt: string | null;
}

export interface KycSubmissionRequest {
  documentType: string;
  documentReference: string;
  nationalId: string;
  dateOfBirth: string; // YYYY-MM-DD
}

// ---------- payments ----------
export interface PaymentAttemptDto {
  id: string;
  masterOrderId: string;
  buyerId: number;
  amount: number;
  currency: string;
  gateway: string;
  gatewayReference: string;
  status: 'INITIATED' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'EXPIRED';
  failureReason: string | null;
  initiatedAt: string;
  completedAt: string | null;
  createdAt: string;
}
