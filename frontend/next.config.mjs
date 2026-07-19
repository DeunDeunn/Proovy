/** @type {import('next').NextConfig} */
const nextConfig = {
  devIndicators: false,
  async rewrites() {
    return [
      {
        source: "/api/auth/:path*",
        destination: `${process.env.BACKEND_URL || "http://localhost:8080"}/auth/:path*`,
      },
      {
        source: "/api/:path*",
        destination: `${process.env.BACKEND_URL || "http://localhost:8080"}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
