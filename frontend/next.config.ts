import type { NextConfig } from "next";

const blobPublicBaseUrl = process.env.NEXT_PUBLIC_AZURE_BLOB_PUBLIC_BASE_URL;
const blobRemotePattern = blobPublicBaseUrl
  ? (() => {
      const url = new URL(blobPublicBaseUrl);
      if (url.protocol !== "https:") {
        throw new Error("NEXT_PUBLIC_AZURE_BLOB_PUBLIC_BASE_URL must use HTTPS");
      }
      return {
        protocol: "https" as const,
        hostname: url.hostname,
        port: url.port,
        pathname: `${url.pathname.replace(/\/+$/, "")}/**`
      };
    })()
  : null;

const nextConfig: NextConfig = {
  output: "standalone",
  reactStrictMode: true,
  poweredByHeader: false,
  images: {
    remotePatterns: blobRemotePattern ? [blobRemotePattern] : []
  }
};

export default nextConfig;
