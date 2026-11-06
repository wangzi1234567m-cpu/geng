import fs from 'node:fs/promises';
import zlib from 'node:zlib';
const root = 'E:/桌面文件/开发/梗图';
const source = await fs.readFile(root + '/docx/双人梗簿_PRD_v0.1.md', 'utf8');
const esc = s => s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
const run = (s, props='') => `<w:r><w:rPr>${props}</w:rPr><w:t xml:space="preserve">${esc(s)}</w:t></w:r>`;
const para = (s, style='Normal') => `<w:p><w:pPr><w:pStyle w:val="${style}"/></w:pPr>${run(s)}</w:p>`;
const lines = source.split(/\r?\n/); let body = '';
for(let i=0;i<lines.length;i++) {
  const l = lines[i].trim(); if(!l) continue;
  if(l==='<!-- page -->') {body += '<w:p><w:r><w:br w:type="page"/></w:r></w:p>'; continue;}
  if(l.startsWith('|')) {
    const rows=[]; while(i<lines.length && lines[i].trim().startsWith('|')) { const cells=lines[i].trim().split('|').slice(1,-1).map(x=>x.trim()); if(!cells.every(x=>/^[- :]+$/.test(x)))rows.push(cells); i++; } i--;
    const n=rows[0].length; const widths=n===5?[1900,1745,1745,1745,1745]:n===3?[1700,4300,2880]:Array(n).fill(Math.floor(8880/n));
    body+='<w:tbl><w:tblPr><w:tblW w:w="8880" w:type="dxa"/><w:tblLayout w:type="fixed"/><w:tblBorders>'+['top','left','bottom','right','insideH','insideV'].map(k=>`<w:${k} w:val="single" w:sz="4" w:color="D9D9D9"/>`).join('')+'</w:tblBorders><w:tblCellMar><w:top w:w="100" w:type="dxa"/><w:left w:w="120" w:type="dxa"/><w:bottom w:w="100" w:type="dxa"/><w:right w:w="120" w:type="dxa"/></w:tblCellMar></w:tblPr><w:tblGrid>'+widths.map(w=>`<w:gridCol w:w="${w}"/>`).join('')+'</w:tblGrid>';
    rows.forEach((row,ri)=>{body+='<w:tr><w:trPr><w:cantSplit/>'+(ri===0?'<w:tblHeader/>':'')+'</w:trPr>';row.forEach((c,ci)=>body+=`<w:tc><w:tcPr><w:tcW w:w="${widths[ci]}" w:type="dxa"/><w:vAlign w:val="center"/>${ri===0?'<w:shd w:fill="E7EEF5"/>':''}</w:tcPr><w:p><w:pPr><w:pStyle w:val="TableText"/></w:pPr>${run(c,ri===0?'<w:b/>':'')}</w:p></w:tc>`);body+='</w:tr>';});body+='</w:tbl>';continue;
  }
  if(l.startsWith('### ')) body+=para(l.slice(4),'Heading2');
  else if(l.startsWith('## ')) body+=para(l.slice(3),'Heading1');
  else if(l.startsWith('# ')) body+=para(l.slice(2),'Title');
  else body+=para(l);
}
const ns='http://schemas.openxmlformats.org/wordprocessingml/2006/main';
const font='<w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Microsoft YaHei"/>';
const st=(id,name,size,bold,spacing)=>`<w:style w:type="paragraph" w:styleId="${id}"><w:name w:val="${name}"/>${id!=='Normal'?'<w:basedOn w:val="Normal"/>':''}<w:pPr>${spacing}${id.startsWith('Heading')||id==='Title'?'<w:keepNext/><w:keepLines/>':''}</w:pPr><w:rPr>${font}<w:sz w:val="${size}"/><w:szCs w:val="${size}"/><w:color w:val="000000"/>${bold?'<w:b/>':''}</w:rPr></w:style>`;
const styles=`<?xml version="1.0" encoding="UTF-8"?><w:styles xmlns:w="${ns}"><w:docDefaults><w:rPrDefault><w:rPr>${font}<w:sz w:val="21"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after="100" w:line="310" w:lineRule="auto"/><w:widowControl/></w:pPr></w:pPrDefault></w:docDefaults>${st('Normal','Normal',21,false,'')}${st('Title','Title',36,true,'<w:spacing w:before="60" w:after="220"/>')}${st('Heading1','heading 1',27,true,'<w:spacing w:before="180" w:after="110"/><w:outlineLvl w:val="0"/>')}${st('Heading2','heading 2',23,true,'<w:spacing w:before="140" w:after="80"/><w:outlineLvl w:val="1"/>')}${st('TableText','Table Text',19,false,'<w:spacing w:after="0" w:line="280" w:lineRule="auto"/>')}</w:styles>`;
const files={
 '[Content_Types].xml':'<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/><Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/></Types>',
 '_rels/.rels':'<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>',
 'word/_rels/document.xml.rels':'<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/></Relationships>',
 'word/styles.xml':styles,
 'word/footer1.xml':`<?xml version="1.0" encoding="UTF-8"?><w:ftr xmlns:w="${ns}"><w:p><w:pPr><w:jc w:val="center"/></w:pPr>${run('双人梗簿 PRD v0.1  ·  ', '<w:sz w:val="18"/>')}<w:fldSimple w:instr="PAGE"/></w:p></w:ftr>`,
 'word/document.xml':`<?xml version="1.0" encoding="UTF-8"?><w:document xmlns:w="${ns}" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><w:body>${body}<w:sectPr><w:footerReference w:type="default" r:id="rId2"/><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1100" w:right="1513" w:bottom="1100" w:left="1513" w:header="400" w:footer="500"/></w:sectPr></w:body></w:document>`
};
const crcTable = Array.from({length:256},(_,i)=>{for(let j=0;j<8;j++)i=(i&1)?0xEDB88320^(i>>>1):i>>>1; return i>>>0;});
const crc32=b=>{let c=0xFFFFFFFF;for(const v of b)c=crcTable[(c^v)&255]^(c>>>8);return (c^0xFFFFFFFF)>>>0;};
let off=0;const chunks=[],central=[];
for(const [name,content]of Object.entries(files)) {const nb=Buffer.from(name),raw=Buffer.from(content),data=zlib.deflateRawSync(raw),crc=crc32(raw),h=Buffer.alloc(30);h.writeUInt32LE(0x04034b50,0);h.writeUInt16LE(20,4);h.writeUInt16LE(8,8);h.writeUInt32LE(crc,14);h.writeUInt32LE(data.length,18);h.writeUInt32LE(raw.length,22);h.writeUInt16LE(nb.length,26);chunks.push(h,nb,data);const c=Buffer.alloc(46);c.writeUInt32LE(0x02014b50,0);c.writeUInt16LE(20,4);c.writeUInt16LE(20,6);c.writeUInt16LE(8,10);c.writeUInt32LE(crc,16);c.writeUInt32LE(data.length,20);c.writeUInt32LE(raw.length,24);c.writeUInt16LE(nb.length,28);c.writeUInt32LE(off,42);central.push(c,nb);off+=h.length+nb.length+data.length;}
const cd=Buffer.concat(central),end=Buffer.alloc(22);end.writeUInt32LE(0x06054b50,0);end.writeUInt16LE(Object.keys(files).length,8);end.writeUInt16LE(Object.keys(files).length,10);end.writeUInt32LE(cd.length,12);end.writeUInt32LE(off,16);
await fs.writeFile(root+'/docx/双人梗簿_PRD_v0.1.docx',Buffer.concat([...chunks,cd,end]));
export const result={file:root+'/docx/双人梗簿_PRD_v0.1.docx',sections:(source.match(/^## /gm)||[]).length,explicitPages:(source.match(/<!-- page -->/g)||[]).length+1};
