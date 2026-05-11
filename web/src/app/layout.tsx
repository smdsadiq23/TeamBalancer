import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "TeamBalancer",
  description: "View-only web client for TeamBalancer",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full flex flex-col font-sans">{children}</body>
    </html>
  );
}
