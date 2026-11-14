package com.verifour.app.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verifour.app.R
import com.verifour.app.i18n.FlysticTranslations
import com.verifour.app.model.AppLocale
import com.verifour.app.model.Cleanliness
import com.verifour.app.model.PlasticType
import com.verifour.app.ui.components.EcoCard
import com.verifour.app.ui.theme.EcoBorder
import com.verifour.app.ui.theme.EcoCoral
import com.verifour.app.ui.theme.EcoForeground
import com.verifour.app.ui.theme.EcoLeaf
import com.verifour.app.ui.theme.EcoMuted
import com.verifour.app.ui.theme.EcoMutedForeground
import com.verifour.app.ui.theme.EcoOcean
import com.verifour.app.ui.theme.EcoPrimary
import com.verifour.app.ui.theme.EcoSecondary
import com.verifour.app.ui.theme.EcoSunshine

private data class PlasticGuideItem(
    val code: String,
    val emoji: String,
    val accepted: Boolean,
    val basePoints: Int?,
    val containerColor: Color,
    val borderColor: Color,
    @DrawableRes val imageRes: Int?,
    val name: String,
    val value: String,
    val features: String,
    val examples: List<String>,
    val imageLabels: List<String> = emptyList()
)

@Composable
fun GuideScreen(
    locale: AppLocale,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    fun t(key: String) = FlysticTranslations.t(key, locale)

    val guides = buildGuides(locale)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
            start = 20.dp,
            end = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = t("backHome"),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = EcoMutedForeground,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = t("guideTitle"),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = EcoForeground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = t("guideDescription"),
                fontSize = 14.sp,
                color = EcoMutedForeground
            )
        }

        items(guides, key = { it.code }) { g ->
            EcoCard(
                containerColor = g.containerColor,
                borderColor = g.borderColor,
                contentPadding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(g.emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = g.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoForeground
                            )
                            Text(
                                text = g.value,
                                fontSize = 11.sp,
                                color = EcoMutedForeground
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (g.accepted && g.basePoints != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, EcoPrimary.copy(alpha = 0.35f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${g.basePoints} ${t("guidePointUnit")}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoPrimary
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, EcoCoral.copy(alpha = 0.35f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = t("guideNotAccepted").uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoCoral
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${t("guideDetailsLabel")} ${g.features}",
                    fontSize = 12.sp,
                    color = EcoForeground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = t("guideCommonItemsLabel"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(4.dp))
                g.examples.forEach { ex ->
                    Text(
                        text = "• $ex",
                        fontSize = 12.sp,
                        color = EcoForeground.copy(alpha = 0.85f),
                        lineHeight = 17.sp
                    )
                }

                if (g.imageRes != null && g.imageLabels.size == 3) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, EcoBorder)
                    ) {
                        Column {
                            Image(
                                painter = painterResource(id = g.imageRes),
                                contentDescription = g.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(2.5f)
                            )
                            HorizontalDivider(color = EcoBorder)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                g.imageLabels.forEach { lbl ->
                                    Text(
                                        text = lbl,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EcoMutedForeground,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cleanliness multipliers
        item {
            EcoCard(contentPadding = 18.dp) {
                Text(
                    text = t("guideQuality"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = t("guideQualityHelp"),
                    fontSize = 12.sp,
                    color = EcoMutedForeground
                )
                Spacer(Modifier.height(12.dp))
                listOf(
                    Cleanliness.CLEAN to t("guideCleanDry"),
                    Cleanliness.NORMAL to t("guideNormal"),
                    Cleanliness.DIRTY to t("guideDirtyWet")
                ).forEach { (c, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EcoSecondary)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EcoForeground)
                        Text("×${c.multiplier}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EcoPrimary)
                    }
                }
            }
        }

        // Formula & example
        item {
            EcoCard(
                containerColor = EcoPrimary.copy(alpha = 0.1f),
                borderColor = EcoPrimary.copy(alpha = 0.25f),
                contentPadding = 18.dp
            ) {
                Text(
                    text = t("guideQuality"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.8f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t("guideFormula"),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoForeground,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = t("guideExampleTitle"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoForeground
                )
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.8f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(t("guideExampleCleanDry"), fontSize = 12.sp, color = EcoForeground)
                    Text(t("guideExampleDirty"), fontSize = 12.sp, color = EcoForeground)
                }
            }
        }
    }
}

private fun buildGuides(locale: AppLocale): List<PlasticGuideItem> {
    return when (locale) {
        AppLocale.VI -> listOf(
            PlasticGuideItem("PET", "🧴", true, PlasticType.PET.basePoints, EcoPrimary.copy(alpha = 0.08f), EcoPrimary.copy(alpha = 0.3f), R.drawable.plastic_pet,
                "PET (Số 1)", "Giá trị tái chế: Rất cao", "Thường trong suốt, nhẹ, cứng vừa phải.",
                listOf("Chai nước suối (Aquafina, Lavie...)", "Chai nước ngọt có ga", "Ly nhựa trong đựng trà sữa (loại mỏng)", "Hộp trong suốt đựng trái cây"),
                listOf("Chai nước suối", "Chai nước ngọt", "Hộp trái cây")
            ),
            PlasticGuideItem("HDPE", "🧼", true, PlasticType.HDPE.basePoints, EcoLeaf.copy(alpha = 0.12f), EcoLeaf.copy(alpha = 0.4f), R.drawable.plastic_hdpe,
                "HDPE (Số 2)", "Giá trị tái chế: Rất cao, an toàn", "Thường đục, màu sắc đậm, cứng cáp, chịu nhiệt tốt.",
                listOf("Chai sữa uống đóng chai", "Chai mỹ phẩm, sữa tắm, dầu gội", "Túi nilon dày từ siêu thị"),
                listOf("Chai sữa đục", "Chai dầu gội", "Túi siêu thị")
            ),
            PlasticGuideItem("PP", "🥤", true, PlasticType.PP.basePoints, EcoSunshine.copy(alpha = 0.16f), EcoSunshine.copy(alpha = 0.45f), R.drawable.plastic_pp,
                "PP (Số 5)", "Giá trị tái chế: Cao", "Chịu nhiệt rất tốt, hơi đục hoặc có màu, dẻo dai.",
                listOf("Cốc trà sữa loại cứng (dập màng)", "Hộp đựng cơm take-away", "Nắp chai nước suối / nước ngọt", "Ống hút nhựa loại tốt"),
                listOf("Ly trà sữa", "Hộp cơm", "Nắp chai")
            ),
            PlasticGuideItem("LDPE", "🛍️", true, PlasticType.LDPE.basePoints, EcoOcean.copy(alpha = 0.10f), EcoOcean.copy(alpha = 0.3f), R.drawable.plastic_ldpe,
                "LDPE (Số 4)", "Giá trị tái chế: Trung bình - Thấp", "Mỏng, mềm dẻo, dễ kéo giãn.",
                listOf("Túi nilon thông thường", "Màng bọc thực phẩm", "Bao bì bọc ngoài lốc sữa, lốc nước ngọt"),
                listOf("Túi nilon", "Màng bọc", "Bao bì lốc sữa")
            ),
            PlasticGuideItem("PS", "🥡", true, PlasticType.PS.basePoints, EcoCoral.copy(alpha = 0.10f), EcoCoral.copy(alpha = 0.3f), R.drawable.plastic_ps,
                "PS (Số 6)", "Giá trị tái chế: Rất thấp", "Nhẹ, xốp, hoặc dạng cứng giòn dễ nứt vỡ.",
                listOf("Hộp xốp đựng xôi, cơm rang, bánh bao", "Muỗng / nĩa / dao nhựa dùng một lần", "Ly nhựa mỏng dùng một lần"),
                listOf("Hộp xốp", "Muỗng / nĩa", "Ly nhựa mỏng")
            ),
            PlasticGuideItem("PVC", "🚫", false, null, EcoMuted, EcoBorder, null,
                "PVC (Số 3) & OTHER (Số 7)", "Khó tái chế, chứa phụ gia phức tạp", "Bao gồm thẻ nhựa, bìa file hồ sơ, bình nước cá nhân dùng nhiều lần.",
                listOf("Thẻ sinh viên, bìa file (PVC)", "Bình nước cá nhân (PC - Số 7)")
            )
        )
        AppLocale.EN -> listOf(
            PlasticGuideItem("PET", "🧴", true, PlasticType.PET.basePoints, EcoPrimary.copy(alpha = 0.08f), EcoPrimary.copy(alpha = 0.3f), R.drawable.plastic_pet,
                "PET (No. 1)", "Recycling value: Very high", "Usually clear, lightweight, and moderately rigid.",
                listOf("Water bottles (Aquafina, Lavie, etc.)", "Carbonated drink bottles", "Thin clear milk-tea cups", "Clear fruit containers"),
                listOf("Water bottle", "Soft-drink bottle", "Fruit container")
            ),
            PlasticGuideItem("HDPE", "🧼", true, PlasticType.HDPE.basePoints, EcoLeaf.copy(alpha = 0.12f), EcoLeaf.copy(alpha = 0.4f), R.drawable.plastic_hdpe,
                "HDPE (No. 2)", "Recycling value: Very high and safe", "Usually opaque, strongly coloured, sturdy, and heat resistant.",
                listOf("Bottled milk containers", "Cosmetic, body-wash, and shampoo bottles", "Thick supermarket bags"),
                listOf("Opaque milk bottle", "Shampoo bottle", "Supermarket bag")
            ),
            PlasticGuideItem("PP", "🥤", true, PlasticType.PP.basePoints, EcoSunshine.copy(alpha = 0.16f), EcoSunshine.copy(alpha = 0.45f), R.drawable.plastic_pp,
                "PP (No. 5)", "Recycling value: High", "Very heat resistant, slightly opaque or coloured, and durable.",
                listOf("Rigid sealed milk-tea cups", "Takeaway food containers", "Water and soft-drink bottle caps", "Sturdy plastic straws"),
                listOf("Milk-tea cup", "Food container", "Bottle cap")
            ),
            PlasticGuideItem("LDPE", "🛍️", true, PlasticType.LDPE.basePoints, EcoOcean.copy(alpha = 0.10f), EcoOcean.copy(alpha = 0.3f), R.drawable.plastic_ldpe,
                "LDPE (No. 4)", "Recycling value: Medium to low", "Thin, soft, flexible, and easy to stretch.",
                listOf("Regular plastic bags", "Food wrap", "Outer packaging for milk and soft-drink multipacks"),
                listOf("Plastic bag", "Food wrap", "Multipack packaging")
            ),
            PlasticGuideItem("PS", "🥡", true, PlasticType.PS.basePoints, EcoCoral.copy(alpha = 0.10f), EcoCoral.copy(alpha = 0.3f), R.drawable.plastic_ps,
                "PS (No. 6)", "Recycling value: Very low", "Lightweight, foamed, or rigid and brittle enough to crack easily.",
                listOf("Foam food boxes", "Single-use plastic spoons, forks, and knives", "Thin single-use plastic cups"),
                listOf("Foam container", "Plastic cutlery", "Thin plastic cup")
            ),
            PlasticGuideItem("PVC", "🚫", false, null, EcoMuted, EcoBorder, null,
                "PVC (No. 3) & OTHER (No. 7)", "Difficult to recycle; contains complex additives", "Includes plastic cards, document folders, and reusable personal water bottles.",
                listOf("Student cards and document folders (PVC)", "Reusable water bottles (PC — No. 7)")
            )
        )
        AppLocale.KO -> listOf(
            PlasticGuideItem("PET", "🧴", true, PlasticType.PET.basePoints, EcoPrimary.copy(alpha = 0.08f), EcoPrimary.copy(alpha = 0.3f), R.drawable.plastic_pet,
                "PET (1번)", "재활용 가치: 매우 높음", "대체로 투명하고 가벼우며 적당히 단단합니다.",
                listOf("생수병(Aquafina, Lavie 등)", "탄산음료병", "얇은 투명 밀크티 컵", "투명 과일 용기"),
                listOf("생수병", "탄산음료병", "과일 용기")
            ),
            PlasticGuideItem("HDPE", "🧼", true, PlasticType.HDPE.basePoints, EcoLeaf.copy(alpha = 0.12f), EcoLeaf.copy(alpha = 0.4f), R.drawable.plastic_hdpe,
                "HDPE (2번)", "재활용 가치: 매우 높고 안전함", "대체로 불투명하고 색이 진하며, 단단하고 내열성이 좋습니다.",
                listOf("병 우유 용기", "화장품·바디워시·샴푸 용기", "두꺼운 마트 비닐봉지"),
                listOf("불투명 우유병", "샴푸 용기", "마트 봉지")
            ),
            PlasticGuideItem("PP", "🥤", true, PlasticType.PP.basePoints, EcoSunshine.copy(alpha = 0.16f), EcoSunshine.copy(alpha = 0.45f), R.drawable.plastic_pp,
                "PP (5번)", "재활용 가치: 높음", "내열성이 매우 좋고, 약간 불투명하거나 색이 있으며 질깁니다.",
                listOf("단단한 밀크티 컵", "포장 음식 용기", "생수·탄산음료 병뚜껑", "단단한 플라스틱 빨대"),
                listOf("밀크티 컵", "음식 용기", "병뚜껑")
            ),
            PlasticGuideItem("LDPE", "🛍️", true, PlasticType.LDPE.basePoints, EcoOcean.copy(alpha = 0.10f), EcoOcean.copy(alpha = 0.3f), R.drawable.plastic_ldpe,
                "LDPE (4번)", "재활용 가치: 중간~낮음", "얇고 부드러우며 유연하고 잘 늘어납니다.",
                listOf("일반 비닐봉지", "식품 포장 랩", "우유·음료 묶음의 겉포장"),
                listOf("비닐봉지", "포장 랩", "묶음 포장")
            ),
            PlasticGuideItem("PS", "🥡", true, PlasticType.PS.basePoints, EcoCoral.copy(alpha = 0.10f), EcoCoral.copy(alpha = 0.3f), R.drawable.plastic_ps,
                "PS (6번)", "재활용 가치: 매우 낮음", "가볍고 스티로폼 형태이거나 단단하지만 쉽게 깨집니다.",
                listOf("스티로폼 음식 용기", "일회용 플라스틱 숟가락·포크·칼", "얇은 일회용 플라스틱 컵"),
                listOf("스티로폼 용기", "플라스틱 식기", "얇은 플라스틱 컵")
            ),
            PlasticGuideItem("PVC", "🚫", false, null, EcoMuted, EcoBorder, null,
                "PVC (3번) 및 OTHER (7번)", "재활용이 어렵고 복합 첨가제가 포함됨", "플라스틱 카드, 서류 파일, 재사용 개인 물병 등이 포함됩니다.",
                listOf("학생증·서류 파일(PVC)", "개인 물병(PC — 7번)")
            )
        )
    }
}
