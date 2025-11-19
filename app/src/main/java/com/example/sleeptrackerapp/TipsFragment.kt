package com.example.sleeptrackerapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Modèle de données pour un conseil/article
data class SleepTip(
    val title: String,
    val description: String,
    val category: String, // "CONSEIL" ou "ARTICLE"
    val linkUrl: String? = null // Optionnel, pour les articles
)

class TipsFragment : Fragment() {

    private lateinit var rvTips: RecyclerView

    companion object {
        fun newInstance() = TipsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tips, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvTips = view.findViewById(R.id.rv_tips)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // Liste de données mockées (statiques)
        val tipsList = listOf(
            SleepTip(
                "La règle des 30 minutes",
                "Évitez les écrans (téléphone, ordinateur) au moins 30 minutes avant le coucher pour favoriser la mélatonine.",
                "CONSEIL"
            ),
            SleepTip(
                "Température idéale",
                "Votre chambre doit être fraîche, idéalement entre 18°C et 20°C, pour faciliter l'endormissement.",
                "CONSEIL"
            ),
            SleepTip(
                "Comprendre les cycles du sommeil",
                "Découvrez comment fonctionnent le sommeil léger, profond et paradoxal dans cet article détaillé.",
                "ARTICLE",
                "https://www.sommeil.org/comprendre-les-cycles" // Lien exemple
            ),
            SleepTip(
                "Caféine et sommeil",
                "La caféine peut rester dans votre système jusqu'à 8 heures. Évitez le café après 14h.",
                "CONSEIL"
            ),
            SleepTip(
                "Méditation guidée pour dormir",
                "Une vidéo relaxante de 10 minutes pour calmer votre esprit avant la nuit.",
                "VIDÉO",
                "https://www.youtube.com/results?search_query=meditation+sommeil"
            )
        )

        rvTips.layoutManager = LinearLayoutManager(context)
        rvTips.adapter = TipsAdapter(tipsList)
    }
}

// Adaptateur pour la liste
class TipsAdapter(private val tips: List<SleepTip>) :
    RecyclerView.Adapter<TipsAdapter.TipViewHolder>() {

    class TipViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategory: TextView = view.findViewById(R.id.tv_tip_category)
        val tvTitle: TextView = view.findViewById(R.id.tv_tip_title)
        val tvDesc: TextView = view.findViewById(R.id.tv_tip_description)
        val btnShare: Button = view.findViewById(R.id.btn_share_tip)
        val btnOpen: Button = view.findViewById(R.id.btn_open_link)
        val ivIcon: ImageView = view.findViewById(R.id.iv_tip_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tip, parent, false)
        return TipViewHolder(view)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        val tip = tips[position]
        val context = holder.itemView.context

        holder.tvTitle.text = tip.title
        holder.tvDesc.text = tip.description
        holder.tvCategory.text = tip.category

        // Gestion de l'icône selon la catégorie
        if (tip.category == "VIDÉO" || tip.category == "ARTICLE") {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_view) // Icône générique ou personnalisée
        } else {
            holder.ivIcon.setImageResource(R.drawable.idea)
        }

        // Gestion du bouton "Lire/Voir"
        if (tip.linkUrl != null) {
            holder.btnOpen.visibility = View.VISIBLE
            holder.btnOpen.text = if (tip.category == "VIDÉO") "Voir la vidéo" else "Lire l'article"

            holder.btnOpen.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tip.linkUrl))
                context.startActivity(intent)
            }
        } else {
            holder.btnOpen.visibility = View.GONE
        }

        // Gestion du bouton "Partager"
        holder.btnShare.setOnClickListener {
            val shareText = "Conseil sommeil : ${tip.title}\n\n${tip.description}\n\nVia Aymen Zemrani SleepTracker App"
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Partager ce conseil"))
        }
    }

    override fun getItemCount() = tips.size
}