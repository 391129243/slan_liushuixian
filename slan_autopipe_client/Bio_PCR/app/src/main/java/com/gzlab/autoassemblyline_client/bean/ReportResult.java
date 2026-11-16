package com.gzlab.autoassemblyline_client.bean;

public class ReportResult {
   private String category; //检测项目、病毒种类
   private String conclusion;//结论、结论
   private String reference;//参考值
   private String ctValue;
   private String rmValue;
   private String tmValue;

   public String getCategory() {
      return category;
   }

   public void setCategory(String category) {
      this.category = category;
   }

   public String getConclusion() {
      return conclusion;
   }

   public void setConclusion(String conclusion) {
      this.conclusion = conclusion;
   }

   public String getReference() {
      return reference;
   }

   public void setReference(String reference) {
      this.reference = reference;
   }

   public String getCtValue() {
      return ctValue;
   }

   public void setCtValue(String ctValue) {
      this.ctValue = ctValue;
   }

   public String getRmValue() {
      return rmValue;
   }

   public void setRmValue(String rmValue) {
      this.rmValue = rmValue;
   }

   public String getTmValue() {
      return tmValue;
   }

   public void setTmValue(String tmValue) {
      this.tmValue = tmValue;
   }
}
