import { apiClient, withIdempotencyKey } from '@/lib/apiClient';
import { useAuthStore } from '@/lib/auth';
import type {
  ApiResponse,
  AuthData,
  CartDto,
  CartItemDto,
  CompleteRepairRequest,
  CreateDisputeRequest,
  CreateInspectionRequest,
  CreateListingRequest,
  CreateRepairTicketDto,
  CreateReturnRequest,
  DisputeResponse,
  EscrowStatsDto,
  FulfillmentOrderDto,
  InspectionResponse,
  KycStatusDto,
  KycSubmissionRequest,
  ListingDto,
  ListingSearchParams,
  MasterOrderDto,
  NotificationResponse,
  NotificationsPageResponse,
  OrderConfirmationResponse,
  Page,
  PaymentAttemptDto,
  ProductModelDto,
  ProfileDto,
  RepairTicketDto,
  ReservationResponse,
  ResolveDisputeRequest,
  ReviewResponse,
  ReturnResponse,
  SellerMetricsResponse,
  ShipRequest,
  SubmitQcRequest,
  SubmitReviewRequest,
  SubmitTradeInRequest,
  TradeInCalculationRequest,
  TradeInOfferResponse,
  TradeInRequestResponse,
  UpdateListingRequest,
  UpdateProfileRequest,
  WarrantyDto,
} from '@/types/api';

function unwrap<T>(response: { data: ApiResponse<T> }): T {
  return response.data.data;
}

// ---------- auth ----------
export async function login(email: string, password: string): Promise<AuthData> {
  const auth = unwrap<AuthData>(await apiClient.post('/auth/login', { email, password }));
  useAuthStore.getState().setSession(auth);
  return auth;
}

export async function register(input: { email: string; password: string; fullName: string; phoneNumber?: string }): Promise<AuthData> {
  const auth = unwrap<AuthData>(await apiClient.post('/auth/register', input));
  useAuthStore.getState().setSession(auth);
  return auth;
}

export function logout() {
  useAuthStore.getState().clearSession();
}

export async function loginWithFirebase(idToken: string): Promise<AuthData> {
  const auth = unwrap<AuthData>(await apiClient.post('/auth/firebase', { idToken }));
  useAuthStore.getState().setSession(auth);
  return auth;
}

// ---------- listings ----------
export async function searchListings(params: ListingSearchParams = {}): Promise<Page<ListingDto>> {
  return unwrap<Page<ListingDto>>(await apiClient.get('/listings/search', { params }));
}

export async function getListing(id: string): Promise<ListingDto> {
  return unwrap<ListingDto>(await apiClient.get(`/listings/${id}`));
}

export async function getMyListings(): Promise<ListingDto[]> {
  return unwrap<ListingDto[]>(await apiClient.get('/listings'));
}

export async function createListing(input: CreateListingRequest): Promise<ListingDto> {
  return unwrap<ListingDto>(await apiClient.post('/listings', input));
}

export async function updateListing(id: string, input: UpdateListingRequest): Promise<ListingDto> {
  return unwrap<ListingDto>(await apiClient.patch(`/listings/${id}`, input));
}

export async function pauseListing(id: string): Promise<ListingDto> {
  return unwrap<ListingDto>(await apiClient.post(`/listings/${id}/pause`));
}

export async function resumeListing(id: string): Promise<ListingDto> {
  return unwrap<ListingDto>(await apiClient.post(`/listings/${id}/resume`));
}

// ---------- checkout ----------
export async function reserveUnit(input: { unitId: string; listingId: string }): Promise<ReservationResponse> {
  return unwrap<ReservationResponse>(await apiClient.post('/checkout/reserve', input));
}

export async function confirmPayment(
  input: { reservationToken: string; paymentMethod: string; shippingAddress: string }
): Promise<OrderConfirmationResponse> {
  return unwrap<OrderConfirmationResponse>(
    await apiClient.post('/checkout/confirm-payment', input, { headers: withIdempotencyKey() })
  );
}

// ---------- orders ----------
export async function getMyOrders(page = 0, size = 20): Promise<Page<MasterOrderDto>> {
  return unwrap<Page<MasterOrderDto>>(await apiClient.get('/orders/my', { params: { page, size } }));
}

export async function getSellerFulfillments(page = 0, size = 20): Promise<Page<FulfillmentOrderDto>> {
  return unwrap<Page<FulfillmentOrderDto>>(await apiClient.get('/orders/seller/my', { params: { page, size } }));
}

export async function shipFulfillment(id: string, input: ShipRequest): Promise<FulfillmentOrderDto> {
  return unwrap<FulfillmentOrderDto>(await apiClient.post(`/orders/fulfillment/${id}/ship`, input));
}

// ---------- admin ----------
export async function getAdminFulfillments(page = 0, size = 20): Promise<Page<FulfillmentOrderDto>> {
  return unwrap<Page<FulfillmentOrderDto>>(await apiClient.get('/admin/fulfillments', { params: { page, size } }));
}

export async function getAdminDisputes(page = 0, size = 20): Promise<Page<DisputeResponse>> {
  return unwrap<Page<DisputeResponse>>(await apiClient.get('/disputes/admin/all', { params: { page, size } }));
}

export async function deliverFulfillment(id: string): Promise<FulfillmentOrderDto> {
  return unwrap<FulfillmentOrderDto>(await apiClient.post(`/orders/fulfillment/${id}/deliver`));
}

export async function completeFulfillment(id: string): Promise<FulfillmentOrderDto> {
  return unwrap<FulfillmentOrderDto>(await apiClient.post(`/orders/fulfillment/${id}/complete`));
}

export async function disbursePayout(id: string): Promise<FulfillmentOrderDto> {
  return unwrap<FulfillmentOrderDto>(await apiClient.post(`/orders/fulfillment/${id}/payout`));
}

export async function resolveDispute(id: string, input: ResolveDisputeRequest): Promise<DisputeResponse> {
  return unwrap<DisputeResponse>(await apiClient.post(`/disputes/${id}/resolve`, input));
}

export async function unlockUser(userId: number): Promise<void> {
  await apiClient.post(`/admin/users/${userId}/unlock`);
}

// ---------- sellers ----------
export async function getSellerMetrics(sellerId: number): Promise<SellerMetricsResponse> {
  return unwrap<SellerMetricsResponse>(await apiClient.get(`/sellers/${sellerId}/metrics`));
}

// ---------- inspections ----------
export async function createInspection(input: CreateInspectionRequest): Promise<InspectionResponse> {
  return unwrap<InspectionResponse>(await apiClient.post('/inspections', input));
}

export async function getLatestInspection(unitId: string): Promise<InspectionResponse> {
  return unwrap<InspectionResponse>(await apiClient.get(`/inspections/unit/${unitId}`));
}

// ---------- disputes (buyer) ----------
export async function getMyDisputes(page = 0, size = 20): Promise<Page<DisputeResponse>> {
  return unwrap<Page<DisputeResponse>>(await apiClient.get('/disputes/my', { params: { page, size } }));
}

export async function createDispute(input: CreateDisputeRequest): Promise<DisputeResponse> {
  return unwrap<DisputeResponse>(await apiClient.post('/disputes', input));
}

// ---------- trade-in ----------
export async function calculateTradeIn(input: TradeInCalculationRequest): Promise<TradeInOfferResponse> {
  return unwrap<TradeInOfferResponse>(await apiClient.post('/trade-in/calculate', input));
}

export async function submitTradeInRequest(input: SubmitTradeInRequest): Promise<TradeInRequestResponse> {
  return unwrap<TradeInRequestResponse>(await apiClient.post('/trade-in/requests', input));
}

export async function getMyTradeInRequests(): Promise<TradeInRequestResponse[]> {
  return unwrap<TradeInRequestResponse[]>(await apiClient.get('/trade-in/requests/my'));
}

// ---------- warranties ----------
export async function getMyWarranties(page = 0, size = 20): Promise<Page<WarrantyDto>> {
  return unwrap<Page<WarrantyDto>>(await apiClient.get('/warranties/my', { params: { page, size } }));
}

// ---------- reviews ----------
export async function submitReview(input: SubmitReviewRequest): Promise<ReviewResponse> {
  return unwrap<ReviewResponse>(await apiClient.post('/reviews', input));
}

export async function getSellerReviews(sellerId: number): Promise<ReviewResponse[]> {
  return unwrap<ReviewResponse[]>(await apiClient.get(`/reviews/seller/${sellerId}`));
}

// ---------- catalog ----------
export async function getProductModels(): Promise<ProductModelDto[]> {
  return unwrap<ProductModelDto[]>(await apiClient.get('/catalog/models'));
}

// ---------- cart ----------
export async function getCart(): Promise<CartDto> {
  return unwrap<CartDto>(await apiClient.get('/cart'));
}

export async function addCartItem(input: { listingId: string; quantity?: number }): Promise<CartItemDto> {
  return unwrap<CartItemDto>(await apiClient.post('/cart/items', input));
}

export async function removeCartItem(listingId: string): Promise<void> {
  await apiClient.delete(`/cart/items/${listingId}`);
}

export async function clearCart(): Promise<void> {
  await apiClient.delete('/cart');
}

// ---------- returns ----------
export async function requestReturn(input: CreateReturnRequest): Promise<ReturnResponse> {
  return unwrap<ReturnResponse>(await apiClient.post('/returns', input));
}

export async function getMyReturns(page = 0, size = 20): Promise<Page<ReturnResponse>> {
  return unwrap<Page<ReturnResponse>>(await apiClient.get('/returns/my', { params: { page, size } }));
}

// ---------- notifications ----------
export async function getMyNotifications(unreadOnly = false, page = 0, size = 20): Promise<NotificationsPageResponse> {
  return unwrap<NotificationsPageResponse>(await apiClient.get('/notifications/my', { params: { unreadOnly, page, size } }));
}

export async function markNotificationRead(id: string): Promise<void> {
  await apiClient.post(`/notifications/${id}/read`);
}

export async function markAllNotificationsRead(): Promise<void> {
  await apiClient.post('/notifications/read-all');
}

// ---------- profile ----------
export async function getMyProfile(): Promise<ProfileDto> {
  return unwrap<ProfileDto>(await apiClient.get('/users/me'));
}

export async function updateMyProfile(input: UpdateProfileRequest): Promise<ProfileDto> {
  return unwrap<ProfileDto>(await apiClient.put('/users/me', input));
}

export async function getMyKycStatus(): Promise<KycStatusDto> {
  return unwrap<KycStatusDto>(await apiClient.get('/users/me/kyc'));
}

export async function submitMyKyc(input: KycSubmissionRequest): Promise<KycStatusDto> {
  return unwrap<KycStatusDto>(await apiClient.post('/users/me/kyc', input));
}

// ---------- payments ----------
export async function getMyPayments(page = 0, size = 20): Promise<Page<PaymentAttemptDto>> {
  return unwrap<Page<PaymentAttemptDto>>(await apiClient.get('/payments/my', { params: { page, size } }));
}

export async function getAdminPayments(buyerId?: number, page = 0, size = 20): Promise<Page<PaymentAttemptDto>> {
  return unwrap<Page<PaymentAttemptDto>>(await apiClient.get('/payments/admin', { params: { buyerId, page, size } }));
}

// ---------- repairs ----------
export async function createRepairTicket(input: CreateRepairTicketDto): Promise<RepairTicketDto> {
  return unwrap<RepairTicketDto>(await apiClient.post('/repairs', input));
}

export async function getMyRepairTickets(page = 0, size = 20): Promise<Page<RepairTicketDto>> {
  return unwrap<Page<RepairTicketDto>>(await apiClient.get('/repairs/my', { params: { page, size } }));
}

export async function getRepairTickets(status?: string, unitId?: string, page = 0, size = 20): Promise<Page<RepairTicketDto>> {
  return unwrap<Page<RepairTicketDto>>(await apiClient.get('/repairs', { params: { status, unitId, page, size } }));
}

export async function repairStartDiagnosis(id: string): Promise<RepairTicketDto> {
  return unwrap<RepairTicketDto>(await apiClient.post(`/repairs/${id}/start-diagnosis`));
}

export async function repairStartRepair(id: string, estimatedPartsCost?: number): Promise<RepairTicketDto> {
  return unwrap<RepairTicketDto>(await apiClient.post(`/repairs/${id}/start-repair`, null, { params: { estimatedPartsCost } }));
}

export async function repairSubmitQc(id: string, input: SubmitQcRequest): Promise<RepairTicketDto> {
  return unwrap<RepairTicketDto>(await apiClient.post(`/repairs/${id}/submit-qc`, input));
}

export async function repairComplete(id: string, input: CompleteRepairRequest): Promise<RepairTicketDto> {
  return unwrap<RepairTicketDto>(await apiClient.post(`/repairs/${id}/complete`, input));
}

export async function repairCancel(id: string, reason: string): Promise<RepairTicketDto> {
  return unwrap<RepairTicketDto>(await apiClient.post(`/repairs/${id}/cancel`, { reason }));
}

// ---------- returns (admin) ----------
export async function getAdminReturns(status?: string, page = 0, size = 20): Promise<Page<ReturnResponse>> {
  return unwrap<Page<ReturnResponse>>(await apiClient.get('/returns/admin', { params: { status, page, size } }));
}

export async function reviewReturn(id: string, input: { approved: boolean; reason?: string }): Promise<ReturnResponse> {
  return unwrap<ReturnResponse>(await apiClient.post(`/returns/${id}/review`, input));
}

export async function recordReturnShipment(id: string, input: { courierName: string; trackingNumber: string }): Promise<ReturnResponse> {
  return unwrap<ReturnResponse>(await apiClient.post(`/returns/${id}/shipment`, input));
}

export async function markReturnReceived(id: string): Promise<ReturnResponse> {
  return unwrap<ReturnResponse>(await apiClient.post(`/returns/${id}/received`));
}

export async function inspectReturn(id: string, input: { notes?: string; refundAmount: number }): Promise<ReturnResponse> {
  return unwrap<ReturnResponse>(await apiClient.post(`/returns/${id}/inspection`, input));
}

export async function finalizeReturnRefund(id: string): Promise<ReturnResponse> {
  return unwrap<ReturnResponse>(await apiClient.post(`/returns/${id}/finalize-refund`));
}

export async function closeReturn(id: string): Promise<ReturnResponse> {
  return unwrap<ReturnResponse>(await apiClient.post(`/returns/${id}/close`));
}

// ---------- escrow (admin) ----------
export async function getEscrowStats(): Promise<EscrowStatsDto> {
  return unwrap<EscrowStatsDto>(await apiClient.get('/escrow/stats'));
}

// ---------- error helper ----------
export function apiErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const err = error as { response?: { data?: { message?: string; details?: string[] } } };
    const payload = err.response?.data;
    if (payload?.details?.length) return `${payload.message}: ${payload.details.join(', ')}`;
    if (payload?.message) return payload.message;
  }
  return 'Something went wrong. Please try again.';
}
