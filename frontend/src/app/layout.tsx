import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import MainNav from "./main-nav";
import "./globals.css";

export const metadata: Metadata = {
  title: "Farmora",
  description: "Organic ecommerce storefront for seamless customer shopping"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>
        <header className="topbar">
          <div className="shell topbarInner">
            <Link href="/" className="brand" aria-label="Farmora home">
              <Image
                src="/farmora-logo.png"
                alt="Farmora"
                width={780}
                height={660}
                priority
                className="brandLogo"
              />
            </Link>
            <MainNav />
          </div>
        </header>
        <div className="shell">{children}</div>
      </body>
    </html>
  );
}
