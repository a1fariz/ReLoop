import type { Metadata } from "next";
import { Plus_Jakarta_Sans, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { Navbar } from "@/components/Navbar";
import { Footer } from "@/components/Footer";
import { Providers } from "@/components/Providers";

const jakarta = Plus_Jakarta_Sans({
  subsets: ["latin"],
  variable: "--font-sans",
  weight: ["300", "400", "500", "600", "700", "800"],
});

const mono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-mono",
  weight: ["400", "500", "600", "700"],
});

export const metadata: Metadata = {
  title: "ReLoop — Certified Circular Hardware Marketplace",
  description: "Enterprise circular tech ecosystem backed by 50-point diagnostic matrix, pessimistic row-lock checkout leases, and double-entry ledger escrow settlement.",
  alternates: {
    canonical: "https://www.reloop.biz.id",
    languages: { "id-ID": "https://www.reloop.biz.id", "en-US": "https://www.reloop.biz.id" },
  },
  openGraph: {
    title: "ReLoop — Certified Circular Hardware Marketplace",
    description: "Certified circular hardware marketplace with verified listings and escrow protection.",
    url: "https://www.reloop.biz.id",
    siteName: "ReLoop",
    type: "website",
  },
  metadataBase: new URL("https://www.reloop.biz.id"),
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className={`${jakarta.variable} ${mono.variable}`}>
      <body className="min-h-screen bg-[#ffffff] font-sans antialiased text-[#1d1d1f] flex flex-col justify-between selection:bg-black selection:text-white">
        <Providers>
          <Navbar />
          <main className="flex-1">{children}</main>
          <Footer />
        </Providers>
      </body>
    </html>
  );
}
