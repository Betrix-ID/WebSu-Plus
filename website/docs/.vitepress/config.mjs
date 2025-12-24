import { defineConfig } from 'vitepress'

export default defineConfig({
  base: '/WebSu-Plus/', 
  
  title: "WebSu Plus",
  description: "Platform Modul Berbasis WebUI",
  
  themeConfig: {
    logo: '/logo.png',

    nav: [
      { text: 'Home', link: '/' },
      { text: 'guide', link: '/guide/WebSu-info' },
      { text: 'Plugin', link: '/plugin/Pugin-websu' },
    ],

    sidebar: [
      {
        text: 'Basic Guide',
        items: [
          { text: 'What is WebSu Plus? ', link: '/guide/WebSu-info' },
          { text: 'Permission Details (Root/Shizuku) ', link: '/guide/info' },
        ]
      },
      {
        text: 'Plugin Documentation',
        items: [
          { text: 'Plugin WebSu', link: '/plugin/Pugin-websu' },
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
