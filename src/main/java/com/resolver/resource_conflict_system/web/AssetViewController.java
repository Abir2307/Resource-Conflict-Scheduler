package com.resolver.resource_conflict_system.web;

import com.resolver.resource_conflict_system.domain.AssetCategory;
import com.resolver.resource_conflict_system.domain.AssetItem;
import com.resolver.resource_conflict_system.service.AssetCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.lang.NonNull;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AssetViewController {

    private final AssetCatalogService assetCatalogService;

    public AssetViewController(AssetCatalogService assetCatalogService) {
        this.assetCatalogService = assetCatalogService;
    }

    @GetMapping("/assets")
    public String assetsPage(Model model) {
        model.addAttribute("assets", assetCatalogService.snapshotAssets());
        return "assets";
    }

    @GetMapping("/assets/new")
    public String newAsset(Model model) {
        model.addAttribute("form", AssetFormData.blank());
        model.addAttribute("action", "/assets/save");
        model.addAttribute("title", "Add Asset");
        return "asset-form";
    }

    @GetMapping("/assets/edit/{id}")
    public String editAsset(@PathVariable @NonNull String id, Model model) {
        return assetCatalogService.findAsset(id)
                .map(asset -> {
                    model.addAttribute("form", AssetFormData.from(asset));
                    model.addAttribute("action", "/assets/save");
                    model.addAttribute("title", "Edit Asset");
                    return "asset-form";
                })
                .orElse("redirect:/assets");
    }

    @PostMapping("/assets/save")
    public String saveAsset(@RequestParam(required = false) String id,
                            @RequestParam String name,
                            @RequestParam String category,
                            @RequestParam int quantity,
                            @RequestParam String unit,
                            @RequestParam String location,
                            @RequestParam(required = false) String notes,
                            @RequestParam(required = false) String dependencyAssetIds,
                            @RequestParam(required = false) java.math.BigDecimal costPerPiece,
                            @RequestParam(required = false) Long estimatedArrivalHours,
                            @RequestParam(required = false) String supplierNodeId) {
        long eta = estimatedArrivalHours == null ? 0L : estimatedArrivalHours.longValue();
        AssetItem asset = new AssetItem(id == null ? "" : id, name, AssetCategory.valueOf(category), quantity, unit, location, notes == null ? "" : notes,
            java.util.List.of(), splitCsvList(dependencyAssetIds), costPerPiece == null ? java.math.BigDecimal.ZERO : costPerPiece, eta, supplierNodeId == null ? "" : supplierNodeId);
        if (asset.id().isBlank()) {
            assetCatalogService.addAsset(asset);
        } else {
            assetCatalogService.updateAsset(asset);
        }
        return "redirect:/assets";
    }

    @PostMapping("/assets/delete")
    public String deleteAsset(@RequestParam String id) {
        assetCatalogService.deleteAsset(id);
        return "redirect:/assets";
    }

    public record AssetFormData(String id, String name, String category, int quantity, String unit, String location, String notes, String dependencyAssetIds, java.math.BigDecimal costPerPiece, long estimatedArrivalHours, String supplierNodeId) {
        static AssetFormData blank() {
            return new AssetFormData("", "", AssetCategory.EQUIPMENT.name(), 1, "units", "Store-A", "", "", java.math.BigDecimal.ZERO, 24L, "Store-A");
        }

        static AssetFormData from(AssetItem asset) {
            return new AssetFormData(asset.id(), asset.name(), asset.category().name(), asset.quantity(), asset.unit(), asset.location(), asset.notes(),
                    String.join(", ", asset.dependencyAssetIds()), asset.costPerPiece(), asset.estimatedArrivalHours(), asset.supplierNodeId());
        }
    }

    private java.util.List<String> splitCsvList(String csv) {
        if (csv == null || csv.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }
}