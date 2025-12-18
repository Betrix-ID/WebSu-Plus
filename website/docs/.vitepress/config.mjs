import { defineConfig } from 'vitepress'

export default defineConfig({
  // WAJIB: Sesuai nama repository di GitHub
  base: '/WebSu-Plus/', 
  
  title: "WebSu Plus",
  description: "Platform Modul Berbasis WebUI",
  
  themeConfig: {
    // VitePress mencari ini di website/docs/public/logo.png
    logo: '/logo.png',

    nav: [
      { text: 'Home', link: '/' },
      { text: 'Informasi', link: '/guide/WebSu-info' },
      { text: 'Plugin', link: '/plugin/Pugin-websu' }
    ],

    sidebar: [
      {
        text: '📖 Panduan Dasar',
        items: [
          { text: 'Apa itu WebSu Plus?', link: '/guide/WebSu-info' },
          { text: 'Detail Izin (Root/Shizuku)', link: '/guide/info' },
        ]
      },
      {
        text: '🧩 Dokumentasi Plugin',
        items: [
          { text: 'Plugin WebSu', link: '/plugin/Pugin-websu' },
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/Betrix-ID/WebSu-Plus' }
    ],

    footer: {
      message: 'Aplikasi Khusus Modul WebUI (Root & Unroot).',
      copyright: 'Copyright © 2025 WebSu Plus'
    }
  }
})
