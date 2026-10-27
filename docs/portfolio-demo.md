# ReLoop: naskah demo dan wawancara

**Format:** panduan presentasi 1–2 menit, bukan rekaman video atau bukti bahwa sesi demo sudah dijalankan.

[Source code](https://github.com/a1fariz/ReLoop) · [Demo web](https://reloop.biz.id) · [Studi kasus](https://alfarizi.my.id/work/reloop)

## Persiapan

Gunakan lingkungan demo/lokal dengan data contoh. Siapkan halaman katalog, detail listing, checkout, dan orders. Untuk penjelasan kode, buka `backend-quarkus/` serta tes ledger dan reservation. Jangan tampilkan kredensial, token, atau data pribadi di layar.

## Alur sekitar 100 detik

| Waktu | Tampilan | Narasi |
| --- | --- | --- |
| 0–15 detik | Katalog dan detail listing | “ReLoop adalah project circular commerce untuk barang elektronik berserial. Saya membangun alur marketplace dengan informasi kondisi barang, checkout, dan pencatatan escrow.” |
| 15–35 detik | Checkout dan sisa waktu reservasi | “Menambahkan barang ke keranjang tidak menahan stok. Saat checkout dimulai, backend membuat lease 15 menit dengan row lock dan constraint database agar reservasi mengikuti aturan yang sama.” |
| 35–55 detik | Orders dan status pesanan | “Alur pesanan menggunakan pencatatan debit/kredit untuk pergerakan escrow. Pembayaran pada project ini memakai mock gateway; ini bukan demonstrasi pemrosesan uang nyata.” |
| 55–80 detik | Struktur `backend-quarkus/` dan tes ledger | “Saya memilih modular monolith agar domain tetap berada dalam satu aplikasi, dengan aturan dependensi ArchUnit. Tes ledger memeriksa penolakan jurnal yang tidak seimbang; tes reservasi memeriksa aturan lease dan kondisi barang.” |
| 80–100 detik | Ringkasan arsitektur dan batas demo | “Outbox memisahkan pencatatan transaksi dari efek samping asinkron. Yang bisa ditinjau adalah kode, aturan database, dan tes; saya tidak mengklaim throughput atau SLA production dari demo ini.” |

## Bukti kode untuk diskusi

- `backend-quarkus/src/test/java/com/reloop/ledger/DoubleEntryLedgerServiceTest.java`: penolakan jurnal tidak seimbang dan settlement multi-line.
- `backend-quarkus/src/test/java/com/reloop/checkout/CheckoutReservationServiceTest.java`: lease dan validasi reservasi.
- `backend-quarkus/src/test/java/com/reloop/architecture/ModuleBoundaryArchitectureTest.java`: aturan dependensi antarmodul beserta coupling yang memang diizinkan.
- `backend-quarkus/src/main/java/com/reloop/payments/controller/PaymentController.java`: endpoint mock gateway dan simulasi hasil pembayaran.

## Pertanyaan wawancara yang perlu disiapkan

- Mengapa memakai modular monolith, dan kapan pemisahan service akan masuk akal?
- Mengapa lease dan constraint database diperlukan selain pengecekan di UI?
- Apa beda invariant debit = kredit dengan bukti bahwa seluruh alur bisnis bebas kesalahan?
- Apa yang terjadi bila pengiriman efek samping outbox harus diulang?

**Batas klaim:** lease 15 menit adalah aturan bisnis; keseimbangan jurnal adalah invariant. Keduanya bukan benchmark latensi, throughput, atau jaminan production.
