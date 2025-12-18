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
      { text: 'Terminal', link: '/guide/Terminal' } // Menambahkan link Terminal di atas
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
      },
      {
        text: '💻 Referensi Teknis',
        items: [
          { text: 'Quick Terminal', link: '/guide/Terminal' } // Menambahkan link ke Terminal.md
        ]
      }
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/Betrix-ID/WebSu-Plus' }
    ],

    footer: {
      message: 'Aplikasi Khusus Modul WebUI (Root & Unroot).',
      copyright: 'Copyright © 2025 WebSu Plus'
    },

    // Fitur pencarian lokal
    search: {
      provider: 'local'
    }
  }
})
