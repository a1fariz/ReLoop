import Link from 'next/link';

export const metadata = { title: 'Legal & Policies — ReLoop' };

const sections = [
  {
    id: 'terms',
    title: 'Syarat Layanan',
    body: 'ReLoop mempertemukan pembeli dan penjual perangkat elektronik tersertifikasi. Platform memverifikasi status unit melalui listing aktif, menampilkan harga penjual, dan mengelola reservasi saat checkout dimulai. Pembayaran pada demo ini tidak memproses dana nyata.',
  },
  {
    id: 'privacy',
    title: 'Privasi',
    body: 'Data akun digunakan untuk autentikasi, pesanan, garansi, dan notifikasi. Informasi KYC dikumpulkan untuk verifikasi penjual dan pembeli. Data tidak dijual kepada pihak ketiga untuk iklan.',
  },
  {
    id: 'returns',
    title: 'Kebijakan Pengembalian',
    body: 'Retur dapat diajukan untuk fulfillment berstatus DELIVERED atau COMPLETED. Kelayakan dan jumlah refund diverifikasi tim platform setelah inspeksi unit. Jumlah refund mengikuti kebijakan yang ditetapkan platform.',
  },
  {
    id: 'warranty',
    title: 'Kebijakan Garansi',
    body: 'Garansi standar diterbitkan saat fulfillment selesai dan dapat batal jika ada retur final. Klaim garansi dan sengketa diselesaikan melalui alur dukungan resmi sesuai ketentuan yang berlaku.',
  },
];

export default function LegalPage() {
  return (
    <main className="min-h-screen bg-[#fafaf9] px-4 py-16 text-stone-950 sm:px-6">
      <div className="mx-auto max-w-3xl">
        <Link href="/" className="editorial-link mb-8 inline-flex">← Kembali</Link>
        <h1 className="text-4xl font-bold tracking-tight">Legal &amp; Kebijakan</h1>
        <div className="mt-10 space-y-8">
          {sections.map((section) => (
            <section key={section.id} id={section.id} className="rounded-3xl border border-stone-200 bg-white p-6 shadow-sm sm:p-8">
              <h2 className="text-xl font-bold">{section.title}</h2>
              <p className="mt-3 text-sm leading-7 text-stone-600">{section.body}</p>
            </section>
          ))}
        </div>
        <p className="mt-10 text-sm text-stone-500">
          Pertanyaan? Hubungi{' '}
          <a href="mailto:alfarizi.developer@gmail.com" className="font-semibold text-sky-800 underline">alfarizi.developer@gmail.com</a>.
        </p>
      </div>
    </main>
  );
}