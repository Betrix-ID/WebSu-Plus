import { defineConfig } from 'vitepress'

export default defineConfig({
  title: "WebSu Plus",
  description: "Platform Modul Berbasis WebUI",
  
  themeConfig: {
    // Logo diambil dari folder public atau folder logo yang tersedia
    logo: '/logo.png',

    // Navigasi Bar Atas
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Informasi', link: '/guide/WebSu-info' },
      { text: 'Plugin', link: '/plugin/Pugin-websu' }
    ],

    // Sidebar Samping
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

    // Link Sosial Media
    socialLinks: [
      { icon: 'github', link: 'https://github.com/Betrix-ID/WebSu-Plus' }
    ],

    // Footer
    footer: {
      message: 'Aplikasi Khusus Modul WebUI (Root & Unroot).',
      copyright: 'Copyright © 2025 WebSu Plus'
    },

    // Fitur Pencarian
    search: {
      provider: 'local'
    }
  }
})
