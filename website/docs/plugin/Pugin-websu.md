# Plugin WebSu (Webui root and unroot)

::: Kesimpulan
kenali Plugin WebSu tanpan batasan izin bagi root dan batasan untuk unroot
:::

## Apa itu Plugin WebSu.?

Plugin WebSu dibangun di atas arsitektur yang sama dengan Plugin Webui root, ini bukan di maksudkan sebagai penganti, melainkan sebagai feature tambahan bagi **WebSu Plus** yang memperkenankan Plugin webui tanpa root di dalam **WebSu Plus**

Pada tahap ini, gagasan tersebut berfungsi sebagai eksplorasi awal: memperluas fondasi modul akar ke dalam bentuk yang dapat beroperasi tanpa akses akar, sambil tetap menjadi bagian dari kerangka kerja modular yang sama.

Di masa depan, pendekatan ini dapat dikembangkan lebih lanjut untuk menyediakan fungsionalitas yang lebih luas dan kasus penggunaan baru, menyeimbangkan antara lingkungan yang berakar (rooted) dan tidak berakar (unrooted).

### Dukungan 

Plugin WebSu sudah di dukung API JavaScript yang hampir universal plus sudah include kernelsu online dan offline, API JavaSript bekerja di atas dukungan ADB shell dan Root 

Pada tahap ini user sudah bebas melakukan pengexecutian command shell dan root secara bersamaan 

Plugin WebSu juga udah di dukung BusyBox yang sangat lengkap bagi user root dan unroot, files yang dapat di execute terletak `/data/user_de/0/com.android.shell/WebSu/sbin/busybox` 
BusyBox mendukung `ASH` Standalone Shell Mode" yang dapat diaktifkan/dinonaktifkan saat runtime. Yang dimaksud dengan Standalone Mode ini adalah bahwa ketika dijalankan di shell ash BusyBox, setiap perintah akan langsung menggunakan applet di dalam BusyBox, terlepas dari apa yang diatur sebagai `PATH`. Misalnya, perintah seperti `ls`, `rm`, **TIDAK** `chmod` akan menggunakan apa yang ada di `PATH` (dalam kasus Android secara default akan menjadi , , dan masing-masing), tetapi akan langsung memanggil applet internal BusyBox. Ini memastikan bahwa skrip selalu berjalan di lingkungan yang dapat diprediksi dan selalu memiliki rangkaian perintah lengkap terlepas dari versi Android mana yang dijalankan. Untuk memaksa perintah agar tidak menggunakan BusyBox, Anda harus memanggil file yang dapat dieksekusi dengan jalur lengkap. `/system/bin/ls` `/system/bin/rm` `/system/bin/chmod`

### Plugin WebSu
Folder plugin AxManager ditempatkan `/data/user_de/0/com.android.shell/WebSu/webui` dengan struktur seperti di bawah ini:

```
  /data/user_de/0/com.android.shell/WebSu/webui
  
  $MOPATH          <---   Folder tersebut diberi nama sesuai dengan ID modul. 
  |
  |      *** Identitas Modul *** 
  |
  |---- module.prop  <---  File ini menyimpan metadata modul. 
  |
  |     *** Daftar Isi Utama ***
  |
  |---- /webroot/index.html  <--- Folder untuk membangun plugin webui
  |
  |     *** Daftar tools folder **
  |
  |---- /system/sbin        <--- Folder tools executi files banery
  |
  |     *** customize.sh diganti dengan Amber.sh ***
  |
  |---- Amber.sh           <--- (Opsional, detail selengkapnya nanti) 
  |
  |
  |      *** service.sh di ganti dengan lossy.sh ***
  |
  |---- lossy.sh             <--- Files Skrip ini akan dieksekusi dalam layanan late_start BOOT_COMPLETED. 
  |---- uninstall.sh          <--- Skrip ini akan dieksekusi ketika WebSu Plus menghapus modul Anda. 
  |
  |         ,* File-file modul lainnya *,
  |______ ,,,
```

### module.prop

module.prop is a configuration file for a module. In AxManager, if a module doesn't contain this file, it won't be recognized as a module. The format of this file is as follows:

```prop{7}
id=<string>
name=<string>
version=<string>
versionCode=<int>
author=<string>
description=<string>
```
- `id` has to match this regular expression: `^[a-zA-Z][a-zA-Z0-9._-]+$`
  Example: ✓ `a_module`, ✓ `a.module`, ✓ `module-101`, ✗ `a module`, ✗ `1_module`, ✗ `-a-module`
  This is the **unique identifier** of your module. You should not change it once published.
- `versionCode` has to be an **integer**. This is used to compare versions.
- Others that were not mentioned above can be any **single line string.**
- Make sure to use the `UNIX (LF)` line break type and not the `Windows (CR+LF)` or `Macintosh (CR)`.

### Penginstallan Plugin

Installer plugin WebSu Plus adalah root modul yang dikemas dalam file ZIP yang dapat diinstal di WebSu Plus. Installer plugin WebSu Plus yang paling sederhana hanyalah sebuah root modul yang dikemas sebagai file ZIP.

### Fungsi

```
ui_print <msg>
    print <msg> to console
    Avoid using 'echo' as it will not display in custom recovery's console

abort <msg>
    print error message <msg> to console and terminate the installation
    Avoid using 'exit' as it will skip the termination cleanup steps

set_perm <target> <owner> <group> <permission> [context]
    if [context] is not set, the default is "u:object_r:system_file:s0"
    this function is a shorthand for the following commands:
       chown owner.group target
       chmod permission target
       chcon context target

set_perm_recursive <directory> <owner> <group> <dirpermission> <filepermission> [context]
    if [context] is not set, the default is "u:object_r:system_file:s0"
    for all files in <directory>, it will call:
       set_perm file owner group filepermission context
    for all directories in <directory> (including itself), it will call:
       set_perm dir owner group dirpermission context
```
 
