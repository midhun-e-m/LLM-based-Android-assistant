package com.example.llmosassistant.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.llmosassistant.R
import com.example.llmosassistant.utils.PdfShareHelper
import io.noties.markwon.Markwon
import java.io.File

class ChatAdapter(
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_USER = 0
        private const val VIEW_ASSISTANT_TEXT = 1
        private const val VIEW_ASSISTANT_IMAGE = 2
        private const val VIEW_ASSISTANT_PDF = 3
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]

        return when {
            message.user -> VIEW_USER
            message.pdfFile != null -> VIEW_ASSISTANT_PDF
            message.imageUrl != null -> VIEW_ASSISTANT_IMAGE
            else -> VIEW_ASSISTANT_TEXT
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {

            VIEW_USER -> {
                val view = inflater.inflate(
                    R.layout.item_user_message,
                    parent,
                    false
                )
                UserViewHolder(view)
            }

            VIEW_ASSISTANT_IMAGE -> {
                val view = inflater.inflate(
                    R.layout.item_assistant_image,
                    parent,
                    false
                )
                AssistantImageViewHolder(view)
            }

            VIEW_ASSISTANT_PDF -> {
                val view = inflater.inflate(
                    R.layout.item_assistant_pdf,
                    parent,
                    false
                )
                AssistantPdfViewHolder(view)
            }

            else -> {
                val view = inflater.inflate(
                    R.layout.item_assistant_message,
                    parent,
                    false
                )
                AssistantTextViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        val message = messages[position]

        when (holder) {

            is UserViewHolder -> {
                holder.messageText.text = message.text ?: ""
                animate(holder.itemView)
            }

            is AssistantTextViewHolder -> {

                val markwon = Markwon.create(holder.itemView.context)
                markwon.setMarkdown(holder.messageText, message.text ?: "")

                holder.copyButton.setOnClickListener {
                    copyToClipboard(
                        holder.itemView.context,
                        message.text ?: ""
                    )
                }

                animate(holder.itemView)
            }

            is AssistantImageViewHolder -> {

                val imageUrl = message.imageUrl

                if (imageUrl != null && imageUrl.startsWith("data:image")) {

                    try {

                        val base64Data = imageUrl.substringAfter(",")

                        val imageBytes = Base64.decode(
                            base64Data,
                            Base64.DEFAULT
                        )

                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                            imageBytes,
                            0,
                            imageBytes.size
                        )

                        Glide.with(holder.itemView.context)
                            .load(bitmap)
                            .into(holder.imageView)

                    } catch (e: Exception) {

                        Toast.makeText(
                            holder.itemView.context,
                            "Failed to load image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {

                    Glide.with(holder.itemView.context)
                        .load(imageUrl)
                        .into(holder.imageView)
                }

                animate(holder.itemView)
            }

            is AssistantPdfViewHolder -> {

                holder.title.text = message.text ?: "PDF Generated"

                val file: File? = message.pdfFile

                holder.downloadButton.setOnClickListener {

                    if (file != null) {

                        try {

                            val uri = FileProvider.getUriForFile(
                                holder.itemView.context,
                                holder.itemView.context.packageName + ".provider",
                                file
                            )

                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }

                            holder.itemView.context.startActivity(intent)

                        } catch (e: Exception) {

                            Toast.makeText(
                                holder.itemView.context,
                                "No PDF viewer found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    } else {

                        Toast.makeText(
                            holder.itemView.context,
                            "PDF not available",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                holder.shareButton.setOnClickListener {

                    if (file != null) {

                        PdfShareHelper.sharePDF(
                            holder.itemView.context,
                            file
                        )

                    } else {

                        Toast.makeText(
                            holder.itemView.context,
                            "PDF not available",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                animate(holder.itemView)
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    // 🔧 FIXED FUNCTION
    fun updateMessage(position: Int, newText: String) {

        if (position in messages.indices) {

            val old = messages[position]

            messages[position] = old.copy(
                text = newText,
                user = false   // ensure assistant bubble
            )

            notifyItemChanged(position)
        }
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    private fun animate(view: View) {
        val animation = AlphaAnimation(0f, 1f)
        animation.duration = 200
        view.startAnimation(animation)
    }

    private fun copyToClipboard(context: Context, text: String) {

        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val clip = ClipData.newPlainText("AI Response", text)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            context,
            "Copied to clipboard",
            Toast.LENGTH_SHORT
        ).show()
    }

    class UserViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val messageText: TextView =
            view.findViewById(R.id.messageText)
    }

    class AssistantTextViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val messageText: TextView =
            view.findViewById(R.id.messageText)

        val copyButton: ImageButton =
            view.findViewById(R.id.copyButton)
    }

    class AssistantImageViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val imageView: ImageView =
            view.findViewById(R.id.generatedImage)
    }

    class AssistantPdfViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val title: TextView =
            view.findViewById(R.id.pdfTitle)

        val downloadButton: Button =
            view.findViewById(R.id.downloadPdfButton)

        val shareButton: Button =
            view.findViewById(R.id.sharePdfButton)
    }
}