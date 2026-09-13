import type { MetadataRoute } from 'next';

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: '*',
      allow: [
        '/',
        '/catalog',
        '/trade-in',
      ],
      disallow: [
        '/checkout/',
        '/login',
        '/register',
        '/admin',
        '/seller',
        '/orders',
        '/profile',
        '/notifications',
        '/repairs',
        '/returns',
        '/payments',
        '/inspections',
        '/warranties',
      ],
    },
    sitemap: 'https://www.reloop.biz.id/sitemap.xml',
  };
}