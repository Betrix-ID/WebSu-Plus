import { defineConfig } from 'vitepress'

export default defineConfig({
  // WAJIB: Sesuai nama repository di GitHub agar CSS/Aset tidak error
  base: '/WebSu-Plus/', 
  
  title: "WebSu Plus",
  description: "Platform Modul Berbasis WebUI",
  
  themeConfig: {
    // Pastikan logo berada di website/docs/public/logo.png
    logo: '/logo.png',

    // Navigasi Bar Atas
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Informasi', link: '/guide/WebSu-info' },
      { text: 'Plugin', link: '/plugin/Pugin-websu' },
      { text: 'Terminal', link: '/plugin/Terminal' } 
    ],

    sidebar: [
      {
        text: '📖 Panduan Dasar',
        items: [
          { text: 'What is WebSu Plus? ', link: '/guide/WebSu-info' },
          { text: 'Permission Details (Root/Shizuku) ', link: '/guide/info' },
        ]
      },
      {
        text: '🧩 Dokumentasi Plugin',
        items: [
          { text: 'Plugin WebSu', link: '/plugin/Pugin-websu' },
        ]
      },
      {
        text: '💻 Referensi Teknis',
        items: [
          { text: 'Quick Terminal', link: '/plugin/Terminal' } 
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/Betrix-ID/WebSu-Plus' }
    ],

    footer: {
      message: 'WebUI Module Special Application (Root & Unroot). ',
      copyright: 'Copyright © 2025 WebSu Plus'
    },

    search: {
      provider: 'local'
    }
  }
})
