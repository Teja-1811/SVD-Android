"""Check native navigation, content completeness and existing view-binding contracts."""
from pathlib import Path
import io, json, re, subprocess, sys, tarfile, xml.etree.ElementTree as ET
root=Path(sys.argv[1]); main=root/'app/src/main';res=main/'res';java=main/'java/com/svd/svdagencies'
A='{http://schemas.android.com/apk/res/android}'
checks=[]
def check(condition,message):
    if not condition:raise AssertionError(message)
    checks.append(message)

layouts={p.name:ET.parse(p).getroot() for p in (res/'layout').glob('*.xml')}
for p in res.rglob('*.xml'):ET.parse(p)
check(True,f'All XML resources parse; {len(layouts)} native layouts checked')
archive=subprocess.run(['git','archive','HEAD','app/src/main/res/layout'],cwd=root,check=True,capture_output=True).stdout
with tarfile.open(fileobj=io.BytesIO(archive)) as files:
    for member in files.getmembers():
        if not member.name.endswith('.xml'):continue
        name=Path(member.name).name
        before=ET.fromstring(files.extractfile(member).read())
        def ids(t):return {n.get(A+'id'):n.tag for n in t.iter() if n.get(A+'id','').startswith('@+id/')}
        old=ids(before);new=ids(layouts[name])
        check(all(new.get(id)==kind for id,kind in old.items()),f'{name}: existing binding IDs and view types retained')
for shell in ('customer.xml','user_portal.xml'):
    found={n.get(A+'id') for n in layouts[shell].iter()}
    check(all('@+id/'+id in found for id in ['customerDrawerLayout','customerToolbar','customerNavigationView','customerBottomNav','customerFragmentContainer']),f'{shell}: portal shell contract complete')
for original,variant in [('customer_companies.xml','user_companies.xml'),('customer_catalog_item_card.xml','user_catalog_item_card.xml')]:
    old={n.get(A+'id'):n.tag for n in layouts[original].iter() if n.get(A+'id')}
    new={n.get(A+'id'):n.tag for n in layouts[variant].iter() if n.get(A+'id')}
    check(all(new.get(id)==kind for id,kind in old.items()),f'{variant}: shared controller binding compatible')
pages=json.loads((res/'raw/information_pages.json').read_text(encoding='utf-8'))
check({p['id'] for p in pages}=={'about','contact','privacy','terms','refund','shipment'},'All six website Quick Links have native content')
for p in pages:
    check(len(p['blocks'])>=10 and len(' '.join(b['text'] for b in p['blocks']))>500,f"{p['id']}: complete page content bundled")
    check(not any('{%' in b['text'] or '{{' in b['text'] for b in p['blocks']),f"{p['id']}: no unexpanded template content")
terms=' '.join(b['text'] for p in pages if p['id']=='terms' for b in p['blocks'])
check(all(s in terms for s in ['Acceptance of Terms','Pricing & Payments','Refunds & Cancellations','Limitation of Liability','Termination']),'Terms include all clauses from the website include')
for menu in ('menu_customer_drawer','menu_user_drawer'):
    tree=ET.parse(res/'menu'/f'{menu}.xml').getroot()
    ids={n.get(A+'id') for n in tree.iter()}
    check(all('@+id/'+id in ids for id in ['nav_about','nav_contact_info','nav_privacy','nav_terms','nav_refund','nav_shipment']),f'{menu}: all six native pages reachable')
manifest=(main/'AndroidManifest.xml').read_text(encoding='utf-8')
sources='\n'.join(p.read_text(encoding='utf-8-sig',errors='replace') for p in java.rglob('*.kt'))
check('WebToolsActivity' not in manifest and 'WebToolsActivity' not in sources,'Website tools screen removed from navigation and manifest')
check('.ui.InformationActivity' in manifest,'Native information screen registered')
check('WebView' not in (java/'ui/InformationActivity.kt').read_text().replace('No HTML/WebView.',''),'Information pages render as native controls')
check('setContentView(R.layout.customer)' not in sources,'Standalone account pages select the authenticated portal shell')
tools=(java/'ui/ToolsActivity.kt').read_text()
check('fun web(' not in tools and 'InformationActivity.intent' in tools,'All Tools destinations are native')
check('https://www.google.com/maps' not in (java/'utils/DeliveryLocationPicker.kt').read_text(),'Map actions use native geo intents')
report={'passed':len(checks),'layouts':len(layouts),'pages':{p['id']:len(p['blocks']) for p in pages},'checks':checks}
if len(sys.argv)>2:Path(sys.argv[2]).write_text(json.dumps(report,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if k!='checks'},indent=2))
