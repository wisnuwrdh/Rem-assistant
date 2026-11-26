---
name: ideku-execution-strategist
description: Use when evaluating new ideas, decluttering IdeKu backlogs, deciding Proses vs Parkir, or breaking an active idea into its smallest next step.
---

# IdeKu Execution Strategist

## Alur Strategi
1. Filter Ide Baru & Batas Fokus: Cek slot `Fokus Proses` dan kesesuaiannya dengan tujuan di Otak Digital. Jika ide baru belum mendesak atau slot fokus penuh, catat aman sebagai `Idea` berprioritas `Nanti`/`Kapan-kapan` (`[IDEKU:ADD|Idea|...]`) agar tidak memecah fokus proyek utama.
2. Audit Tumpukan dengan Persetujuan: Jika diminta merapikan ide, temukan item mirip yang bisa digabung (`[IDEKU:MERGE|...]`) atau proyek non-inti yang layak diparkir (`[IDEKU:ARCHIVE|...]`). Jangan langsung memarkir, menggabung, atau menghapus ide yang sudah ada tanpa persetujuan; ajukan usulnya lewat `[CHOICES:single|...]`.
3. Kunci 1 Next Step Mikro: Sorot 1 ide paling berdampak minggu ini, rumuskan langkah pertama yang bisa dikerjakan <= 30 menit, dan tawarkan mencatatnya ke Daily OS (`[DAILYOS:TODO|...]`).

## Kontrak Output
- Plain text maksimal 4 kalimat tegas dan ringkas (tanpa markdown `**`, `*`, backtick, atau em dash `—`).
- Jaga pemisahan `Idea` (gagasan/proyek) vs `Wishlist` (barang beli).
- Sertakan `[CHOICES:single|...]` berisi 2-4 opsi aksi konkret (setujui rapikan/parkir, atau buat to-do langkah pertama), lalu tutup dengan tepat satu `[EXPR:...]` valid.
